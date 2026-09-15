package com.omnichat.arena.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** All target providers. Parked/dropped ones stay in the enum but hidden from pickers. */
enum class ProviderId(val displayName: String) {
    GEMINI("Gemini"),
    DEEPSEEK("DeepSeek"),
    DUCK("Duck.ai"),
    PERPLEXITY("Perplexity"),
    CLAUDE("Claude"),
    GROK("Grok"),
    ARENA("Arena"),
    POLLINATIONS("Pollinations (free)"),
    GROQ("Groq (free tier)"),
    FAKE("Demo (offline)"),
}

/** Selectable in Chat/Arena pickers. Parked (Duck, DeepSeek) / dropped (Arena) engines stay hidden. */
val ProviderId.selectable: Boolean
    get() = when (this) {
        ProviderId.DUCK, ProviderId.ARENA, ProviderId.DEEPSEEK -> false // bot-walled, parked (T5a)
        else -> true
    }

enum class AuthType { API_KEY, SESSION, NONE }

data class ModelInfo(val id: String, val label: String)

data class ChatMessage(val role: String, val text: String) {
    companion object {
        fun user(text: String) = ChatMessage("user", text)
        fun model(text: String) = ChatMessage("model", text)
    }
}

data class ChatRequest(
    val history: List<ChatMessage>,
    val model: String? = null,
    val maxTokens: Int = 2048,
    val temperature: Double = 0.7,
)

/** Streaming events. UI must never know whether a provider is API- or session-based. */
sealed interface StreamEvent {
    data class Token(val delta: String) : StreamEvent
    data class Done(val fullText: String, val latencyMs: Long) : StreamEvent
    data class Error(val message: String, val retryable: Boolean = true) : StreamEvent
}

data class Health(val ok: Boolean, val detail: String)

/**
 * THE interface. Every provider — official API or login-session — implements this.
 * Rules: no WebViews, no plaintext secrets (use SecretStore), SSE → Token flow.
 */
interface AiProvider {
    val id: ProviderId
    val authType: AuthType
    fun models(): List<ModelInfo>
    fun streamChat(request: ChatRequest): Flow<StreamEvent>
    suspend fun healthCheck(): Health
}

/** Offline demo provider so the app runs with zero keys. Also used for UI previews/tests. */
class FakeProvider @Inject constructor() : AiProvider {
    override val id = ProviderId.FAKE
    override val authType = AuthType.NONE
    override fun models() = listOf(ModelInfo("fake-1", "Demo model"))
    override suspend fun healthCheck() = Health(true, "demo")

    override fun streamChat(request: ChatRequest): Flow<StreamEvent> = flow {
        val prompt = request.history.lastOrNull()?.text.orEmpty()
        val start = System.currentTimeMillis()
        val answer = "Demo answer (offline) to: \"$prompt\"\n\nFor real AI: Gemini Pro session + " +
            "Groq free key in Keys tab; Groq also judges arenas."
        for (word in answer.split(" ")) {
            emit(StreamEvent.Token("$word "))
            delay(25)
        }
        emit(StreamEvent.Done(answer, System.currentTimeMillis() - start))
    }
}
