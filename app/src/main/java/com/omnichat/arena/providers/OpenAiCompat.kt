package com.omnichat.arena.providers

import com.omnichat.arena.core.ChatRequest
import com.omnichat.arena.core.StreamEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Shared OpenAI-compatible streaming client with non-streaming fallback.
 * Used by: Groq, Pollinations (and later: any OpenAI-style endpoint).
 * Pure OkHttp + SSE — no SDK needed.
 */
internal object OpenAiCompat {
    private const val BUDGET_ERROR = "free key ran out of budget — try again later, or use Groq (free key) or Demo."

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

        val streamPayload = """{"model":"$model","messages":[$msgs],"stream":true,"max_tokens":${request.maxTokens},"temperature":${request.temperature}}"""
        val streamBody = streamPayload.toRequestBody("application/json".toMediaType())
        val streamBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", DuckAiProvider.UA) // some free endpoints ignore bare-script UAs
            .post(streamBody)
        if (!apiKey.isNullOrBlank()) streamBuilder.header("Authorization", "Bearer $apiKey")

        val sb = StringBuilder()
        var budgetHit = false

        try {
            http.newCall(streamBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    emit(StreamEvent.Error("$tag HTTP ${resp.code}", resp.code in 429..599))
                    return@flow
                }
                Sse.consume(resp, { payload ->
                    when (val res = OpenAiParser.parseStreamDelta(payload, sb.toString())) {
                        is OpenAiParser.StreamDeltaResult.Token -> {
                            sb.append(res.text)
                            emit(StreamEvent.Token(res.text))
                        }
                        is OpenAiParser.StreamDeltaResult.BudgetExhausted -> {
                            budgetHit = true
                        }
                        is OpenAiParser.StreamDeltaResult.EmptyOrNoDelta -> {}
                    }
                }, this)
            }

            if (budgetHit) {
                // UI drops any partial streaming line on Error — user sees only this card.
                emit(StreamEvent.Error("$tag's $BUDGET_ERROR", retryable = true))
                return@flow
            }

            if (sb.isNotEmpty()) {
                emit(StreamEvent.Done(sb.toString(), System.currentTimeMillis() - start))
                return@flow
            }

            // HTTP-200 streaming closed with no usable token.
            // Execute at most one non-streaming fallback request:
            val fallbackPayload = """{"model":"$model","messages":[$msgs],"stream":false,"max_tokens":${request.maxTokens},"temperature":${request.temperature}}"""
            val fallbackBody = fallbackPayload.toRequestBody("application/json".toMediaType())
            val fallbackBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", DuckAiProvider.UA)
                .post(fallbackBody)
            if (!apiKey.isNullOrBlank()) fallbackBuilder.header("Authorization", "Bearer $apiKey")

            var fallbackSuccess = false
            var fallbackHttpCode = 0
            var fallbackResult: OpenAiParser.NonStreamResult? = null

            http.newCall(fallbackBuilder.build()).execute().use { fallbackResp ->
                fallbackHttpCode = fallbackResp.code
                fallbackSuccess = fallbackResp.isSuccessful
                if (fallbackSuccess) {
                    val rawBody = fallbackResp.body?.string().orEmpty()
                    fallbackResult = OpenAiParser.parseNonStreamResponse(rawBody)
                }
            }

            if (!fallbackSuccess) {
                emit(StreamEvent.Error("$tag HTTP $fallbackHttpCode", fallbackHttpCode in 429..599))
                return@flow
            }

            when (val res = fallbackResult) {
                is OpenAiParser.NonStreamResult.Success -> {
                    emit(StreamEvent.Token(res.content))
                    emit(StreamEvent.Done(res.content, System.currentTimeMillis() - start))
                }
                is OpenAiParser.NonStreamResult.BudgetExhausted -> {
                    emit(StreamEvent.Error("$tag's $BUDGET_ERROR", retryable = true))
                }
                is OpenAiParser.NonStreamResult.Error, null -> {
                    emit(StreamEvent.Error("$tag: empty or malformed fallback response", retryable = true))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(StreamEvent.Error("$tag network error: ${e.javaClass.simpleName}", retryable = true))
        }
    }.flowOn(Dispatchers.IO)
}
