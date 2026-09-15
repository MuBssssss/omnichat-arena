package com.omnichat.arena.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** One provider's completed answer in an arena run. */
data class ArenaAnswer(
    val providerId: ProviderId,
    val modelName: String,
    val text: String,
    val latencyMs: Long,
    val error: String? = null,
)

data class JudgeScore(
    val providerId: ProviderId,
    /** 0..100 blended score */
    val score: Float,
    val fastSignals: Map<String, Float>,
    var llmScore: Float? = null,
)

data class JudgeVerdict(
    val winner: ProviderId,
    val scores: List<JudgeScore>,
    val rationale: String,
    /** Optional fused "best of all" answer written by the judge model. */
    val fusedAnswer: String? = null,
)

/**
 * Judge v0: instant on-device fast signals.
 * Judge v1 (TODO): send anonymized, order-shuffled answers to the Groq-free
 * provider as $0 LLM-judge with [JUDGE_PROMPT], parse scores, fuse best answer.
 */
@Singleton
class JudgeEngine @Inject constructor() {

    fun fastJudge(prompt: String, answers: List<ArenaAnswer>): JudgeVerdict {
        if (answers.isEmpty()) {
            return JudgeVerdict(
                winner = ProviderId.FAKE,
                scores = emptyList(),
                rationale = "No answers to evaluate.",
            )
        }
        val scored = answers.map { a ->
            if (a.error != null || a.text.isBlank()) {
                return@map JudgeScore(a.providerId, 0f, mapOf("error" to 0f))
            }
            val signals = mutableMapOf<String, Float>()
            // Length sanity: prefer substantive but not bloated (300–3000 chars ideal).
            val len = a.text.length.toFloat()
            signals["length"] = when {
                len < 50 -> 10f
                len < 300 -> 60f
                len <= 3000 -> 100f
                len <= 8000 -> 75f
                else -> 50f
            }
            // Refusal detection (cheap keyword pass; judge model does the real call).
            val lower = a.text.lowercase()
            val refused = listOf(
                "i can't help", "i cannot help", "i'm unable to",
                "as an ai", "i don't have access"
            ).any { lower.contains(it) }
            signals["refusal"] = if (refused) 0f else 100f
            // Structure: headers/lists/code = effort signal.
            val structureHits = listOf("##", "- ", "1.", "```", "**").count { lower.contains(it) }
            signals["structure"] = (structureHits * 25).coerceAtMost(100).toFloat()
            // Latency: faster is slightly better (tie-break only, 10% weight).
            signals["speed"] = when {
                a.latencyMs < 5_000 -> 100f
                a.latencyMs < 15_000 -> 80f
                a.latencyMs < 30_000 -> 60f
                else -> 40f
            }
            val blended = (signals["length"]!! * 0.35f +
                signals["refusal"]!! * 0.35f +
                signals["structure"]!! * 0.20f +
                signals["speed"]!! * 0.10f)
            JudgeScore(a.providerId, (blended * 10).roundToInt() / 10f, signals)
        }
        val winner = scored.maxByOrNull { it.score }?.providerId ?: answers.firstOrNull()?.providerId ?: ProviderId.FAKE
        return JudgeVerdict(
            winner = winner,
            scores = scored.sortedByDescending { it.score },
            rationale = "Fast on-device signals only (length/refusal/structure/speed).",
        )
    }

    /** Prepares an anonymized and randomly shuffled judge prompt for Groq. */
    fun buildJudgePrompt(prompt: String, answers: List<ArenaAnswer>): Pair<String, Map<String, ProviderId>>? {
        val valid = answers.filter { it.error == null && it.text.isNotBlank() }
        if (valid.isEmpty()) return null
        val shuffled = valid.shuffled()
        val letterMap = mutableMapOf<String, ProviderId>()
        val sb = StringBuilder()
        sb.append(JUDGE_PROMPT.trim()).append("\n\n")
        sb.append("User prompt: \"").append(prompt).append("\"\n\n")
        shuffled.forEachIndexed { idx, a ->
            val letter = ('A' + idx).toString()
            letterMap[letter] = a.providerId
            sb.append("Answer ").append(letter).append(":\n").append(a.text).append("\n\n")
        }
        return sb.toString() to letterMap
    }

    /** Defensively parses the LLM judge JSON response and merges with fast signals. */
    fun parseLlmVerdict(
        rawResponse: String,
        letterMap: Map<String, ProviderId>,
        fastVerdict: JudgeVerdict,
    ): JudgeVerdict {
        val start = rawResponse.indexOf('{')
        val end = rawResponse.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) {
            return fastVerdict.copy(rationale = "${fastVerdict.rationale} (LLM judge output was not JSON)")
        }
        val jsonStr = rawResponse.substring(start, end + 1)
        return try {
            val obj = Json.parseToJsonElement(jsonStr).jsonObject

            val scoresObj = obj["scores"]?.let { elem ->
                runCatching { elem.jsonObject }.getOrNull()
            }
            val winnerLetter = obj["winner"]?.let { elem ->
                runCatching { elem.jsonPrimitive.content.trim() }.getOrNull()
            }
            val rawRationale = obj["rationale"]?.let { elem ->
                runCatching { elem.jsonPrimitive.content }.getOrNull()
            } ?: "LLM judge evaluated answers."

            var deanonRationale = rawRationale
            letterMap.forEach { (letter, pid) ->
                deanonRationale = deanonRationale
                    .replace(Regex("""\b(?:Answer|Contender)\s+$letter\b""", RegexOption.IGNORE_CASE), pid.displayName)
                    .replace("'$letter'", pid.displayName)
                    .replace("\"$letter\"", pid.displayName)
                    .replace("($letter)", "(${pid.displayName})")
                if (letter != "A") {
                    deanonRationale = deanonRationale.replace(Regex("""\b$letter\b"""), pid.displayName)
                } else {
                    deanonRationale = deanonRationale.replace(Regex("""\bA's\b"""), "${pid.displayName}'s")
                }
            }

            val fused = obj["fused"]?.let { elem ->
                runCatching { elem.jsonPrimitive.content.takeIf { it.isNotBlank() } }.getOrNull()
            }

            val llmScores = mutableMapOf<ProviderId, Float>()
            scoresObj?.forEach { (letter, element) ->
                val pid = letterMap[letter.uppercase()] ?: return@forEach
                val num = runCatching { element.jsonPrimitive.content.toFloatOrNull() }.getOrNull() ?: 0f
                llmScores[pid] = num
            }

            val updatedScores = fastVerdict.scores.map { s ->
                val llm = llmScores[s.providerId]
                if (llm != null) {
                    s.copy(score = llm, llmScore = llm)
                } else {
                    s
                }
            }.sortedByDescending { it.score }

            val winner = (winnerLetter?.let { letterMap[it.uppercase()] })
                ?: updatedScores.firstOrNull()?.providerId
                ?: fastVerdict.winner

            JudgeVerdict(
                winner = winner,
                scores = updatedScores,
                rationale = deanonRationale,
                fusedAnswer = fused,
            )
        } catch (e: Exception) {
            fastVerdict.copy(rationale = "${fastVerdict.rationale} (LLM judge JSON parse error)")
        }
    }

    companion object {
        /** Prompt template for the Phase-3 LLM judge. Answers MUST be shuffled + anonymized. */
        const val JUDGE_PROMPT = """
You are an impartial AI judge. User prompt and anonymized answers (A, B, C...) follow.
Score EACH answer 0-100 on: correctness (40%), completeness (25%), citations/evidence (15%), clarity (20%).
Write a concise rationale explaining the score differences and winner, and synthesize a single "fused" best answer combining the best parts.
Reply ONLY as a single valid JSON object with NO markdown code fences:
{"scores":{"A":85,"B":92},"winner":"B","rationale":"Rationale here...","fused":"Merged best answer..."}
"""
    }
}
