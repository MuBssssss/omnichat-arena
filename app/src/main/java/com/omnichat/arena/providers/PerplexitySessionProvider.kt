package com.omnichat.arena.providers

import com.omnichat.arena.core.AiProvider
import com.omnichat.arena.core.AuthType
import com.omnichat.arena.core.ChatRequest
import com.omnichat.arena.core.Health
import com.omnichat.arena.core.ModelInfo
import com.omnichat.arena.core.ProviderId
import com.omnichat.arena.core.StreamEvent
import com.omnichat.arena.data.SecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

internal object PerplexityParser {
    val json = Json { ignoreUnknownKeys = true }

    sealed interface ParseResult {
        data class TextDelta(val delta: String, val newAccumulated: String) : ParseResult
        object AuthWall : ParseResult
        object NoDelta : ParseResult
    }

    fun parseLine(payload: String, accumulatedText: String): ParseResult {
        return try {
            val root = json.parseToJsonElement(payload).jsonObject
            val upsell = root["upsell_information"]?.jsonObject
            if (upsell != null && upsell["name"]?.jsonPrimitive?.content == "fraud_authwall_upsell") {
                return ParseResult.AuthWall
            }

            val blocks = root["blocks"]?.jsonArray ?: return ParseResult.NoDelta
            for (blockElem in blocks) {
                val block = blockElem.jsonObject
                val md = block["markdown_block"]?.jsonObject ?: continue
                val chunks = md["chunks"]?.jsonArray ?: continue
                for (chunkElem in chunks) {
                    val chunk = chunkElem.jsonPrimitive.content
                    if (chunk.isEmpty()) continue
                    if (chunk.startsWith(accumulatedText)) {
                        val delta = chunk.substring(accumulatedText.length)
                        if (delta.isNotEmpty()) {
                            return ParseResult.TextDelta(delta, chunk)
                        }
                    } else if (chunk != accumulatedText) {
                        val delta = chunk
                        val newAccumulated = accumulatedText + chunk
                        return ParseResult.TextDelta(delta, newAccumulated)
                    }
                }
            }
            ParseResult.NoDelta
        } catch (_: Exception) {
            ParseResult.NoDelta
        }
    }
}

/**
 * Perplexity AI session provider ($0-only, unofficial web-session).
 * Communicates with Perplexity SSE backend via __Secure-next-auth.session-token.
 * 100% native OkHttp SSE streaming.
 */
@Singleton
class PerplexitySessionProvider @Inject constructor(
    private val http: OkHttpClient,
    private val secrets: SecretStore,
) : AiProvider {
    override val id = ProviderId.PERPLEXITY
    override val authType = AuthType.SESSION

    companion object {
        const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
        const val SESSION_URL = "https://www.perplexity.ai/api/auth/session"
        const val ASK_URL = "https://www.perplexity.ai/rest/sse/perplexity_ask"
    }

    override fun models() = listOf(
        ModelInfo("turbo", "Perplexity Turbo (Default)"),
    )

    override suspend fun healthCheck(): Health {
        val token = secrets.getRaw("PERPLEXITY_SESSION_TOKEN") ?: secrets.get(id)
        if (token.isNullOrBlank()) {
            return Health(false, "Perplexity session token missing. Add in Keys tab.")
        }
        val request = Request.Builder()
            .url(SESSION_URL)
            .header("User-Agent", UA)
            .header("Accept", "application/json")
            .header("Origin", "https://www.perplexity.ai")
            .header("Referer", "https://www.perplexity.ai/")
            .header("Cookie", "__Secure-next-auth.session-token=$token; next-auth.session-token=$token;")
            .get()
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Health(false, "Session check failed (HTTP ${response.code})")
                } else {
                    val body = response.body?.string() ?: ""
                    val root = PerplexityParser.json.parseToJsonElement(body).jsonObject
                    if (root.containsKey("user")) {
                        Health(true, "Session active")
                    } else {
                        Health(false, "Session expired or unauthenticated. Re-login required.")
                    }
                }
            }
        } catch (e: Exception) {
            Health(false, "Connection error: ${e.javaClass.simpleName}")
        }
    }

    override fun streamChat(request: ChatRequest): Flow<StreamEvent> = flow {
        val token = secrets.getRaw("PERPLEXITY_SESSION_TOKEN") ?: secrets.get(id)
        if (token.isNullOrBlank()) {
            emit(StreamEvent.Error("Perplexity: session token missing. Enter __Secure-next-auth.session-token in Keys tab.", false))
            return@flow
        }

        val prompt = request.history.lastOrNull { it.role == "user" }?.text ?: ""
        if (prompt.isBlank()) {
            emit(StreamEvent.Error("Perplexity: empty prompt.", false))
            return@flow
        }

        val payload = buildJsonObject {
            put("query_str", prompt)
            put("mode", "CONCISE")
            put("model", request.model ?: "turbo")
            put("source", "default")
        }

        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(ASK_URL)
            .header("User-Agent", UA)
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .header("Origin", "https://www.perplexity.ai")
            .header("Referer", "https://www.perplexity.ai/")
            .header("Cookie", "__Secure-next-auth.session-token=$token; next-auth.session-token=$token;")
            .post(body)
            .build()

        val t0 = System.currentTimeMillis()
        var accumulated = ""
        var authWallEncountered = false

        try {
            val response = http.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                val code = response.code
                response.close()
                if (code == 401 || code == 403) {
                    emit(StreamEvent.Error("Perplexity: session expired or rejected (HTTP $code). Re-login in Keys tab.", false))
                } else {
                    emit(StreamEvent.Error("Perplexity error HTTP $code", true))
                }
                return@flow
            }

            Sse.consume(
                response = response,
                onData = { payloadStr ->
                    when (val res = PerplexityParser.parseLine(payloadStr, accumulated)) {
                        is PerplexityParser.ParseResult.AuthWall -> {
                            authWallEncountered = true
                        }
                        is PerplexityParser.ParseResult.TextDelta -> {
                            accumulated = res.newAccumulated
                            emit(StreamEvent.Token(res.delta))
                        }
                        is PerplexityParser.ParseResult.NoDelta -> {}
                    }
                },
                collector = this
            )

            val latency = System.currentTimeMillis() - t0
            if (authWallEncountered) {
                emit(StreamEvent.Error("Perplexity: session expired or auth-wall triggered. Please update session token in Keys tab.", false))
            } else if (accumulated.isNotBlank()) {
                emit(StreamEvent.Done(accumulated, latency))
            } else {
                emit(StreamEvent.Error("Perplexity: empty response received from server.", true))
            }
        } catch (e: Exception) {
            emit(StreamEvent.Error("Perplexity network failure: ${e.javaClass.simpleName}", true))
        }
    }.flowOn(Dispatchers.IO)
}
