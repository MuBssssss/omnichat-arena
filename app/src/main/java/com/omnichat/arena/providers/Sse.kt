package com.omnichat.arena.providers

import kotlinx.coroutines.flow.FlowCollector
import com.omnichat.arena.core.StreamEvent
import kotlinx.serialization.json.Json
import okhttp3.Response

/** Minimal SSE reader: turns `data: {...}` lines into parsed JSON payloads. */
internal object Sse {
    val json = Json { ignoreUnknownKeys = true }

    suspend fun consume(
        response: Response,
        onData: suspend FlowCollector<StreamEvent>.(payload: String) -> Unit,
        collector: FlowCollector<StreamEvent>,
    ) {
        response.body?.source()?.use { src ->
            val buf = okio.Buffer()
            while (!src.exhausted()) {
                val line = src.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty() || payload == "[DONE]") continue
                collector.onData(payload)
            }
        }
    }
}
