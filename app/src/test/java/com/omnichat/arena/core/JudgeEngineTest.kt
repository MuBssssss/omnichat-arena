package com.omnichat.arena.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JudgeEngineTest {

    private lateinit var judge: JudgeEngine

    @Before
    fun setUp() {
        judge = JudgeEngine()
    }

    // --- FastJudge Tests ---

    @Test
    fun fastJudge_emptyAnswers_returnsSafeDefaultVerdictWithoutCrashing() {
        val verdict = judge.fastJudge("test prompt", emptyList())
        assertEquals(ProviderId.FAKE, verdict.winner)
        assertTrue(verdict.scores.isEmpty())
        assertTrue(verdict.rationale.contains("No answers"))
    }

    @Test
    fun fastJudge_normalAnswers_picksHighestScoringContender() {
        val answers = listOf(
            ArenaAnswer(
                providerId = ProviderId.GEMINI,
                modelName = "gemini-pro",
                text = "## Heading\n\nThis is a substantive structured answer with evidence.\n- Point 1\n- Point 2\n\n```python\nprint(1)\n```",
                latencyMs = 2_000,
            ),
            ArenaAnswer(
                providerId = ProviderId.GROQ,
                modelName = "llama-3",
                text = "Short text.",
                latencyMs = 12_000,
            ),
        )

        val verdict = judge.fastJudge("Explain something", answers)
        assertEquals(ProviderId.GEMINI, verdict.winner)
        assertEquals(2, verdict.scores.size)
        assertTrue(verdict.scores[0].score > verdict.scores[1].score)
        assertEquals(ProviderId.GEMINI, verdict.scores[0].providerId)
    }

    @Test
    fun fastJudge_errorAndBlankAnswers_scoredZero() {
        val answers = listOf(
            ArenaAnswer(
                providerId = ProviderId.GEMINI,
                modelName = "gemini-pro",
                text = "",
                latencyMs = 500,
                error = "HTTP 500 connection refused",
            ),
            ArenaAnswer(
                providerId = ProviderId.GROQ,
                modelName = "llama-3",
                text = "   \n\t ",
                latencyMs = 800,
            ),
        )

        val verdict = judge.fastJudge("Test prompt", answers)
        assertEquals(2, verdict.scores.size)
        assertEquals(0f, verdict.scores[0].score, 0.001f)
        assertEquals(0f, verdict.scores[1].score, 0.001f)
        assertEquals(0f, verdict.scores[0].fastSignals["error"] ?: -1f, 0.001f)
    }

    @Test
    fun fastJudge_refusalDetected_scoresZeroOnRefusalSignal() {
        val answers = listOf(
            ArenaAnswer(
                providerId = ProviderId.GEMINI,
                modelName = "gemini-pro",
                text = "I cannot help with that request as an AI assistant. I do not have access to that information.",
                latencyMs = 1_000,
            )
        )

        val verdict = judge.fastJudge("Sensitive query", answers)
        val score = verdict.scores.first()
        assertEquals(0f, score.fastSignals["refusal"] ?: -1f, 0.001f)
    }

    @Test
    fun fastJudge_latencySignals_calculatesTiersCorrectly() {
        val fastAnswer = ArenaAnswer(ProviderId.GEMINI, "model", "Answer of normal length exceeding fifty chars for signal.", 3_000)
        val slowAnswer = ArenaAnswer(ProviderId.GROQ, "model", "Answer of normal length exceeding fifty chars for signal.", 35_000)

        val fastScore = judge.fastJudge("p", listOf(fastAnswer)).scores.first()
        val slowScore = judge.fastJudge("p", listOf(slowAnswer)).scores.first()

        assertEquals(100f, fastScore.fastSignals["speed"] ?: 0f, 0.001f)
        assertEquals(40f, slowScore.fastSignals["speed"] ?: 0f, 0.001f)
    }

    @Test
    fun fastJudge_structureSignals_calculatesStructureHitsProperly() {
        val structured = ArenaAnswer(
            ProviderId.GEMINI,
            "m",
            "## Section 1\n- Item\n1. Numbered\n```code```\n**Bold text**",
            2000
        )
        val verdict = judge.fastJudge("p", listOf(structured))
        assertEquals(100f, verdict.scores.first().fastSignals["structure"] ?: 0f, 0.001f)
    }

    // --- BuildJudgePrompt Tests ---

    @Test
    fun buildJudgePrompt_allValidAnswers_includesAllAndBuildsLetterMap() {
        val answers = listOf(
            ArenaAnswer(ProviderId.GEMINI, "gemini", "Gemini response text", 1000),
            ArenaAnswer(ProviderId.GROQ, "groq", "Groq response text", 1200),
            ArenaAnswer(ProviderId.PERPLEXITY, "perplexity", "Perplexity response text", 1500),
        )

        val result = judge.buildJudgePrompt("Synthetic user prompt", answers)
        assertNotNull(result)
        val (promptText, letterMap) = result!!

        assertEquals(3, letterMap.size)
        assertTrue(letterMap.containsKey("A"))
        assertTrue(letterMap.containsKey("B"))
        assertTrue(letterMap.containsKey("C"))
        assertTrue(letterMap.values.containsAll(listOf(ProviderId.GEMINI, ProviderId.GROQ, ProviderId.PERPLEXITY)))

        assertTrue(promptText.contains("Synthetic user prompt"))
        assertTrue(promptText.contains("Answer A:"))
        assertTrue(promptText.contains("Answer B:"))
        assertTrue(promptText.contains("Answer C:"))
    }

    @Test
    fun buildJudgePrompt_invalidAndErrorAnswers_excludesThemFromPromptAndMap() {
        val answers = listOf(
            ArenaAnswer(ProviderId.GEMINI, "gemini", "Valid answer text", 1000),
            ArenaAnswer(ProviderId.GROQ, "groq", "", 1200, error = "HTTP 429"),
            ArenaAnswer(ProviderId.PERPLEXITY, "perplexity", "   \n\t", 1500),
        )

        val result = judge.buildJudgePrompt("Prompt", answers)
        assertNotNull(result)
        val (promptText, letterMap) = result!!

        assertEquals(1, letterMap.size)
        assertEquals(ProviderId.GEMINI, letterMap["A"])
        assertTrue(promptText.contains("Valid answer text"))
        assertTrue(!promptText.contains("HTTP 429"))
    }

    @Test
    fun buildJudgePrompt_noValidAnswers_returnsNull() {
        val answers = listOf(
            ArenaAnswer(ProviderId.GROQ, "groq", "", 1200, error = "Failed"),
            ArenaAnswer(ProviderId.PERPLEXITY, "perplexity", "   ", 1500),
        )
        val result = judge.buildJudgePrompt("Prompt", answers)
        assertNull(result)
    }

    @Test
    fun buildJudgePrompt_emptyAnswers_returnsNull() {
        val result = judge.buildJudgePrompt("Prompt", emptyList())
        assertNull(result)
    }

    // --- ParseLlmVerdict Tests ---

    @Test
    fun parseLlmVerdict_validJson_parsesScoresWinnerAndFusedAnswer() {
        val rawResponse = """
            Here is the evaluation:
            {"scores":{"A":82.5,"B":91.0},"winner":"B","rationale":"Answer B was much clearer than Answer A.","fused":"Combined synthesis answer"}
        """.trimIndent()

        val letterMap = mapOf("A" to ProviderId.GEMINI, "B" to ProviderId.GROQ)
        val fastVerdict = JudgeVerdict(
            winner = ProviderId.GEMINI,
            scores = listOf(
                JudgeScore(ProviderId.GEMINI, 75f, emptyMap()),
                JudgeScore(ProviderId.GROQ, 70f, emptyMap()),
            ),
            rationale = "Fast signals.",
        )

        val finalVerdict = judge.parseLlmVerdict(rawResponse, letterMap, fastVerdict)

        assertEquals(ProviderId.GROQ, finalVerdict.winner)
        assertEquals("Combined synthesis answer", finalVerdict.fusedAnswer)
        assertEquals(91.0f, finalVerdict.scores[0].score, 0.001f)
        assertEquals(ProviderId.GROQ, finalVerdict.scores[0].providerId)
        assertEquals(82.5f, finalVerdict.scores[1].score, 0.001f)
        assertEquals(ProviderId.GEMINI, finalVerdict.scores[1].providerId)

        // De-anonymized rationale check
        assertTrue(finalVerdict.rationale.contains(ProviderId.GROQ.displayName))
        assertTrue(finalVerdict.rationale.contains(ProviderId.GEMINI.displayName))
        assertTrue(!finalVerdict.rationale.contains("Answer B"))
    }

    @Test
    fun parseLlmVerdict_nonJsonText_fallsBackToFastVerdict() {
        val rawResponse = "I cannot evaluate these models because I am offline."
        val letterMap = mapOf("A" to ProviderId.GEMINI)
        val fastVerdict = JudgeVerdict(
            winner = ProviderId.GEMINI,
            scores = listOf(JudgeScore(ProviderId.GEMINI, 80f, emptyMap())),
            rationale = "Fast signals.",
        )

        val verdict = judge.parseLlmVerdict(rawResponse, letterMap, fastVerdict)
        assertEquals(ProviderId.GEMINI, verdict.winner)
        assertTrue(verdict.rationale.contains("not JSON"))
    }

    @Test
    fun parseLlmVerdict_malformedJson_fallsBackToFastVerdict() {
        val rawResponse = "{scores: [invalid, json, structure}, winner: B"
        val letterMap = mapOf("A" to ProviderId.GEMINI, "B" to ProviderId.GROQ)
        val fastVerdict = JudgeVerdict(
            winner = ProviderId.GEMINI,
            scores = listOf(JudgeScore(ProviderId.GEMINI, 80f, emptyMap())),
            rationale = "Fast signals.",
        )

        val verdict = judge.parseLlmVerdict(rawResponse, letterMap, fastVerdict)
        assertEquals(ProviderId.GEMINI, verdict.winner)
        assertTrue(verdict.rationale.contains("parse error"))
    }

    @Test
    fun parseLlmVerdict_unknownLetters_ignoresUnknownAndPicksTopValidScore() {
        val rawResponse = """
            {"scores":{"A":80,"Z":99},"winner":"Z","rationale":"Contender Z scored highest."}
        """.trimIndent()

        val letterMap = mapOf("A" to ProviderId.GEMINI, "B" to ProviderId.GROQ)
        val fastVerdict = JudgeVerdict(
            winner = ProviderId.GEMINI,
            scores = listOf(
                JudgeScore(ProviderId.GEMINI, 75f, emptyMap()),
                JudgeScore(ProviderId.GROQ, 70f, emptyMap()),
            ),
            rationale = "Fast signals.",
        )

        val verdict = judge.parseLlmVerdict(rawResponse, letterMap, fastVerdict)
        // Z is unknown, so winner falls back to updatedScores top (GEMINI with 80)
        assertEquals(ProviderId.GEMINI, verdict.winner)
        assertEquals(80f, verdict.scores[0].score, 0.001f)
    }

    @Test
    fun parseLlmVerdict_missingScoreFields_preservesFastScoreForMissingContender() {
        val rawResponse = """
            {"scores":{"A":95},"winner":"A","rationale":"Evaluated only A"}
        """.trimIndent()

        val letterMap = mapOf("A" to ProviderId.GEMINI, "B" to ProviderId.GROQ)
        val fastVerdict = JudgeVerdict(
            winner = ProviderId.GROQ,
            scores = listOf(
                JudgeScore(ProviderId.GROQ, 70f, emptyMap()),
                JudgeScore(ProviderId.GEMINI, 60f, emptyMap()),
            ),
            rationale = "Fast signals.",
        )

        val verdict = judge.parseLlmVerdict(rawResponse, letterMap, fastVerdict)
        assertEquals(ProviderId.GEMINI, verdict.winner)
        assertEquals(95f, verdict.scores[0].score, 0.001f) // updated by LLM
        assertEquals(70f, verdict.scores[1].score, 0.001f) // preserved from fastVerdict
        assertEquals(ProviderId.GROQ, verdict.scores[1].providerId)
    }

    @Test
    fun parseLlmVerdict_blankOrMissingFusedAnswer_setsFusedAnswerToNull() {
        val rawResponse = """
            {"scores":{"A":85,"B":90},"winner":"B","rationale":"Good","fused":"   \n\t "}
        """.trimIndent()

        val letterMap = mapOf("A" to ProviderId.GEMINI, "B" to ProviderId.GROQ)
        val fastVerdict = JudgeVerdict(
            winner = ProviderId.GROQ,
            scores = listOf(
                JudgeScore(ProviderId.GROQ, 90f, emptyMap()),
                JudgeScore(ProviderId.GEMINI, 85f, emptyMap()),
            ),
            rationale = "Fast signals.",
        )

        val verdict = judge.parseLlmVerdict(rawResponse, letterMap, fastVerdict)
        assertNull(verdict.fusedAnswer)
    }

    @Test
    fun parseLlmVerdict_deanonSpecialHandlingForA_avoidsReplacingWordA() {
        val rawResponse = """
            {"scores":{"A":85,"B":90},"winner":"B","rationale":"Contender A provided a solid overview, but Contender B had a broader scope."}
        """.trimIndent()

        val letterMap = mapOf("A" to ProviderId.GEMINI, "B" to ProviderId.GROQ)
        val fastVerdict = JudgeVerdict(
            winner = ProviderId.GROQ,
            scores = emptyList(),
            rationale = "Fast signals.",
        )

        val verdict = judge.parseLlmVerdict(rawResponse, letterMap, fastVerdict)
        // "Contender A" replaced with "Gemini"
        assertTrue(verdict.rationale.contains("Gemini provided a solid overview"))
        // "Contender B" replaced with "Groq (free tier)"
        assertTrue(verdict.rationale.contains("Groq (free tier) had a broader scope"))
        // lowercase "a" preserved
        assertTrue(verdict.rationale.contains(" a solid "))
        assertTrue(verdict.rationale.contains(" a broader "))
    }
}
