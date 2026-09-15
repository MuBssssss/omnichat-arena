package com.omnichat.arena.providers

import com.omnichat.arena.core.AiProvider
import com.omnichat.arena.core.AuthType
import com.omnichat.arena.core.ChatRequest
import com.omnichat.arena.core.Health
import com.omnichat.arena.core.ModelInfo
import com.omnichat.arena.core.ProviderId
import com.omnichat.arena.core.StreamEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Duck.ai via UNOFFICIAL endpoints (no official API exists).
 *
 * ⚠️ SPIKE 2026-09-14: the old `status → x-vqd-4 → chat` flow is DEAD.
 * `GET /status` now returns `x-vqd-hash-1`: a base64 JS *challenge* ("DuckDuckGo
 * Fraud & Abuse") that must be EXECUTED (JS runtime + DOM stubs → SHA-256 token)
 * before chatting. Plain-HTTP posts get HTTP 418 ERR_CHALLENGE. Even the latest
 * public solver (p2d-duck 1.3.1) 418s from datacenter IPs — may pass from a real
 * phone network. See docs/spike-duckai-2026-09-14.md.
 *
 * Android plan: embed QuickJS-Android/J2V8 + stubs, solve per session, then SSE.
 * Until the home-IP check passes, treat this provider as STRETCH (MVP = Gemini + DeepSeek).
 * Fully native OkHttp + JS engine — no WebView.
 */
@Singleton
class DuckAiProvider @Inject constructor(
    private val http: OkHttpClient,
) : AiProvider {
    override val id = ProviderId.DUCK
    override val authType = AuthType.NONE

    override fun models() = listOf(
        // TODO(spike): confirm current slugs — these rotate (gpt-*/claude-*/llama-*/mixtral-*).
        ModelInfo("gpt-4o-mini", "GPT-4o mini (via Duck.ai)"),
        ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku (via Duck.ai)"),
        ModelInfo("meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo", "Llama 3.1 70B (via Duck.ai)"),
    )

    override suspend fun healthCheck(): Health = runCatching {
        fetchVqd()
        Health(true, "vqd token OK")
    }.getOrElse { Health(false, it.message ?: "status failed") }

    private fun fetchVqd(): String {
        val req = Request.Builder()
            .url("$BASE/status")
            .header("X-Vqd-Accept", "1")
            .header("User-Agent", UA)
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Duck.ai status HTTP ${resp.code}")
            return resp.header("x-vqd-4")
                ?: resp.header("x-vqd-hash-1")
                ?: error("Duck.ai: no vqd token in response headers")
        }
    }

    override fun streamChat(request: ChatRequest): Flow<StreamEvent> = flow {
        val start = System.currentTimeMillis()
        try {
            val vqd = fetchVqd()
            val model = request.model ?: "gpt-4o-mini"
            val msgs = request.history.joinToString(",") { m ->
                val role = if (m.role == "user") "user" else "assistant"
                """{"role":"$role","content":${Json.encodeToString(m.text)}}"""
            }
            val body = """{"model":"$model","messages":[$msgs]}"""
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$BASE/chat")
                .header("x-vqd-4", vqd)
                .header("User-Agent", UA)
                .post(body)
                .build()
            val sb = StringBuilder()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string()?.take(300)
                    // 429 = anonymous rate limit — tell user plainly.
                    emit(StreamEvent.Error("Duck.ai HTTP ${resp.code}: $err", resp.code != 418))
                    return@flow
                }
                Sse.consume(resp, { payload ->
                    runCatching {
                        val msg = Sse.json.parseToJsonElement(payload).jsonObject["message"]
                            ?.jsonPrimitive?.content
                        if (!msg.isNullOrEmpty()) {
                            sb.append(msg)
                            emit(StreamEvent.Token(msg))
                        }
                    }
                }, this)
            }
            emit(StreamEvent.Done(sb.toString(), System.currentTimeMillis() - start))
        } catch (e: Exception) {
            emit(StreamEvent.Error("Duck.ai: ${e.message ?: e.javaClass.simpleName}", true))
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        // TODO(spike): confirm — historically duckduckgo.com/duckchat/v1; may now be duck.ai/...
        const val BASE = "https://duckduckgo.com/duckchat/v1"
        const val UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
    }
}
