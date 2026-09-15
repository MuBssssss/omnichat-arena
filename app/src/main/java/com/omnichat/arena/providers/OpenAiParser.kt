package com.omnichat.arena.providers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared parser for OpenAI-compatible streaming and non-streaming responses.
 * Detects budget markers and extracts text deltas/messages safely without leaking secrets.
 */
internal object OpenAiParser {
    val json = Json { ignoreUnknownKeys = true }

    /** Free endpoints return budget exhaustion as HTTP 200 + friendly text — catch it. */
    val BUDGET_MARKERS = listOf("reached its budget", "raise the key budget", "agent_key_budget")

    sealed interface NonStreamResult {
        data class Success(val content: String) : NonStreamResult
        data object BudgetExhausted : NonStreamResult
        data class Error(val reason: String) : NonStreamResult
    }

    sealed interface StreamDeltaResult {
        data class Token(val text: String) : StreamDeltaResult
        data object BudgetExhausted : StreamDeltaResult
        data object EmptyOrNoDelta : StreamDeltaResult
    }

    fun parseNonStreamResponse(payload: String): NonStreamResult {
        return try {
            val root = json.parseToJsonElement(payload).jsonObject
            val content = root["choices"]
                ?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.content
            if (content.isNullOrBlank()) {
                NonStreamResult.Error("Empty or blank message content")
            } else if (BUDGET_MARKERS.any { content.contains(it, ignoreCase = true) }) {
                NonStreamResult.BudgetExhausted
            } else {
                NonStreamResult.Success(content)
            }
        } catch (e: Exception) {
            NonStreamResult.Error("Malformed JSON response")
        }
    }

    fun parseStreamDelta(payload: String, accumulatedSoFar: String = ""): StreamDeltaResult {
        return try {
            val root = json.parseToJsonElement(payload).jsonObject
            val delta = root["choices"]
                ?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("delta")?.jsonObject
                ?.get("content")?.jsonPrimitive?.content
            if (delta.isNullOrEmpty()) {
                StreamDeltaResult.EmptyOrNoDelta
            } else if (BUDGET_MARKERS.any { (accumulatedSoFar + delta).contains(it, ignoreCase = true) }) {
                StreamDeltaResult.BudgetExhausted
            } else {
                StreamDeltaResult.Token(delta)
            }
        } catch (e: Exception) {
            StreamDeltaResult.EmptyOrNoDelta
        }
    }
}
