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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ⛔ DISABLED 2026-09-14: paid API violates the user's $0-only mandate.
 * Unbound from DI. DeepSeek returns as a FREE session login (chat.deepseek.com).
 * (Kept for reference; delete when the session provider lands.)
 */
@Singleton
class DeepSeekProvider @Inject constructor(
    private val http: OkHttpClient,
    private val secrets: SecretStore,
) : AiProvider {
    override val id = ProviderId.DEEPSEEK
    override val authType = AuthType.API_KEY

    override fun models() = listOf(
        ModelInfo("deepseek-chat", "DeepSeek V4 Flash (deepseek-chat)"),
        ModelInfo("deepseek-reasoner", "DeepSeek Reasoner"),
    )

    override suspend fun healthCheck(): Health {
        val key = secrets.get(id)
        return if (key.isNullOrBlank()) Health(false, "no API key — add one in Keys tab")
        else Health(true, "key present")
    }

    override fun streamChat(request: ChatRequest): Flow<StreamEvent> = flow {
        val key = secrets.get(id)
        if (key.isNullOrBlank()) {
            emit(StreamEvent.Error("DeepSeek: no API key. Keys tab → DeepSeek.", false))
            return@flow
        }
        val start = System.currentTimeMillis()
        val model = request.model ?: "deepseek-chat"
        val msgs = request.history.joinToString(",") { m ->
            val role = if (m.role == "user") "user" else "assistant"
            """{"role":"$role","content":${Json.encodeToString(m.text)}}"""
        }
        val body = """{"model":"$model","messages":[$msgs],"stream":true,"max_tokens":${request.maxTokens},"temperature":${request.temperature}}"""
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .header("Authorization", "Bearer $key")
            .post(body)
        val sb = StringBuilder()
        try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string()?.take(300)
                    emit(StreamEvent.Error("DeepSeek HTTP ${resp.code}: $err", resp.code in 429..599))
                    return@flow
                }
                Sse.consume(resp, { payload ->
                    runCatching {
                        val delta = Sse.json.parseToJsonElement(payload).jsonObject["choices"]
                            ?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("delta")?.jsonObject
                            ?.get("content")?.jsonPrimitive?.content
                        // reasoner models stream reasoning_content; fall back gracefully
                        val reasoning = Sse.json.parseToJsonElement(payload).jsonObject["choices"]
                            ?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("delta")?.jsonObject
                            ?.get("reasoning_content")?.jsonPrimitive?.content
                        val piece = delta ?: reasoning
                        if (!piece.isNullOrEmpty()) {
                            sb.append(piece)
                            emit(StreamEvent.Token(piece))
                        }
                    }
                }, this)
            }
            emit(StreamEvent.Done(sb.toString(), System.currentTimeMillis() - start))
        } catch (e: Exception) {
            emit(StreamEvent.Error("DeepSeek: ${e.message ?: e.javaClass.simpleName}", true))
        }
    }.flowOn(Dispatchers.IO)
}
