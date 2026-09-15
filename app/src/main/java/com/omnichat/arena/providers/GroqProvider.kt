package com.omnichat.arena.providers

import com.omnichat.arena.core.AiProvider
import com.omnichat.arena.core.AuthType
import com.omnichat.arena.core.ChatRequest
import com.omnichat.arena.core.Health
import com.omnichat.arena.core.ModelInfo
import com.omnichat.arena.core.ProviderId
import com.omnichat.arena.core.StreamEvent
import com.omnichat.arena.data.SecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Groq — FREE tier (generous), key from console.groq.com, OpenAI-compatible.
 * Very fast inference (Llama etc.). Role in app: free engine + PRIMARY judge
 * (judging must cost the user $0, so the judge defaults to Groq-free).
 */
@Singleton
class GroqProvider @Inject constructor(
    private val http: OkHttpClient,
    private val secrets: SecretStore,
) : AiProvider {
    override val id = ProviderId.GROQ
    override val authType = AuthType.API_KEY

    // Verified live 2026-09-14 via /models (llama-3.x retired by Groq).
    override fun models() = listOf(
        ModelInfo("openai/gpt-oss-120b", "GPT-OSS 120B (Groq free tier)"),
        ModelInfo("qwen/qwen3.8-27b", "Qwen 3.8 27B (Groq free tier)"),
        ModelInfo("groq/compound-mini", "Compound Mini fast (Groq free tier)"),
    )

    override suspend fun healthCheck(): Health =
        if (secrets.has(id)) Health(true, "key present")
        else Health(false, "free key at console.groq.com → Keys tab")

    override fun streamChat(request: ChatRequest): Flow<StreamEvent> {
        val key = secrets.get(id)
        if (key.isNullOrBlank()) {
            return flow {
                emit(StreamEvent.Error("Groq: no key. Free key at console.groq.com → Keys tab.", false))
            }
        }
        return OpenAiCompat.stream(
            http = http,
            tag = "Groq",
            url = "https://api.groq.com/openai/v1/chat/completions",
            apiKey = key,
            model = request.model ?: "openai/gpt-oss-120b",
            request = request,
        )
    }
}
