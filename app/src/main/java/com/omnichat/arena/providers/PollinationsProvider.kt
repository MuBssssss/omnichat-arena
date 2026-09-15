package com.omnichat.arena.providers

import com.omnichat.arena.core.AiProvider
import com.omnichat.arena.core.AuthType
import com.omnichat.arena.core.ChatRequest
import com.omnichat.arena.core.Health
import com.omnichat.arena.core.ModelInfo
import com.omnichat.arena.core.ProviderId
import com.omnichat.arena.core.StreamEvent
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pollinations.ai — FREE shared models, no key, no login.
 * OpenAI-compatible endpoint, verified working 2026-09-14 (user's home network).
 * Role in app: instant free engine + free judge fallback. NOT a replacement for
 * the user's own subscriptions (different, weaker shared models).
 */
@Singleton
class PollinationsProvider @Inject constructor(
    private val http: OkHttpClient,
) : AiProvider {
    override val id = ProviderId.POLLINATIONS
    override val authType = AuthType.NONE

    override fun models() = listOf(
        ModelInfo("openai", "Pollinations default (free, no key)"),
    )

    override suspend fun healthCheck() = Health(true, "no key needed")

    override fun streamChat(request: ChatRequest): Flow<StreamEvent> =
        OpenAiCompat.stream(
            http = http,
            tag = "Pollinations",
            url = "https://text.pollinations.ai/openai",
            apiKey = "none",
            model = request.model ?: "openai",
            request = request,
        )
}
