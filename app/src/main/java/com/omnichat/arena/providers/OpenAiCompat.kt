package com.omnichat.arena.providers

import com.omnichat.arena.core.ChatRequest
import com.omnichat.arena.core.StreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Shared OpenAI-compatible streaming client.
 * Used by: Groq, Pollinations (and later: any OpenAI-style endpoint).
 * Pure OkHttp + SSE — no SDK needed.
 */
internal object OpenAiCompat {
    /** Free endpoints return budget exhaustion as HTTP 200 + friendly text — catch it. */
    private val BUDGET_MARKERS = listOf("reached its budget", "raise the key budget", "agent_key_budget")

    fun stream(
        http: OkHttpClient,
        tag: String,
        url: String,
        apiKey: String?,
        model: String,
        request: ChatRequest,
    ): Flow<StreamEvent> = flow {
        val start = System.currentTimeMillis()
        val msgs = request.history.joinToString(",") { m ->
            val role = if (m.role == "user") "user" else "assistant"
            """{"role":"$role","content":${Json.encodeToString(m.text)}}"""
        }
        val body = """{"model":"$model","messages":[$msgs],"stream":true,"max_tokens":${request.maxTokens},"temperature":${request.temperature}}"""
            .toRequestBody("application/json".toMediaType())
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", DuckAiProvider.UA) // some free endpoints ignore bare-script UAs
            .post(body)
        if (!apiKey.isNullOrBlank()) builder.header("Authorization", "Bearer $apiKey")
        val sb = StringBuilder()
        var budgetHit = false
        try {
            http.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string()?.take(300)
                    emit(StreamEvent.Error("$tag HTTP ${resp.code}: $err", resp.code in 429..599))
                    return@flow
                }
                Sse.consume(resp, { payload ->
                    runCatching {
                        val delta = Sse.json.parseToJsonElement(payload).jsonObject["choices"]
                            ?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("delta")?.jsonObject
                            ?.get("content")?.jsonPrimitive?.content
                        if (!delta.isNullOrEmpty()) {
                            sb.append(delta)
                            if (!budgetHit && BUDGET_MARKERS.any { sb.contains(it) }) budgetHit = true
                            else emit(StreamEvent.Token(delta))
                        }
                    }
                }, this)
            }
            if (budgetHit) {
                // UI drops any partial streaming line on Error — user sees only this card.
                emit(StreamEvent.Error(
                    "$tag's free key ran out of budget — try again later, or use Groq (free key) or Demo.",
                    retryable = true
                ))
                return@flow
            }
            if (sb.isEmpty()) {
                // TODO: some free endpoints ignore stream:true and return one JSON blob.
                // Fallback: re-request non-streaming and parse choices[0].message.content.
                emit(StreamEvent.Error("$tag: empty stream (endpoint may not support SSE)", true))
                return@flow
            }
            emit(StreamEvent.Done(sb.toString(), System.currentTimeMillis() - start))
        } catch (e: Exception) {
            emit(StreamEvent.Error("$tag: ${e.message ?: e.javaClass.simpleName}", true))
        }
    }.flowOn(Dispatchers.IO)
}
