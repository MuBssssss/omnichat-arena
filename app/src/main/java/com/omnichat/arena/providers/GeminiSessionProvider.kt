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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gemini via browser session cookies (__Secure-1PSID + __Secure-1PSIDTS).
 * Unlocks real Gemini Pro models ($0-only, no API card required).
 * 100% native OkHttp + batchexecute parser — zero WebViews in chat.
 */
@Singleton
class GeminiSessionProvider @Inject constructor(
    private val http: OkHttpClient,
    private val secrets: SecretStore,
) : AiProvider {
    override val id = ProviderId.GEMINI
    override val authType = AuthType.SESSION

    companion object {
        const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
        private val SNLM_REGEX = Pattern.compile("\"SNlM0e\":\\s*\"([^\"]+)\"")
        private val BL_REGEX = Pattern.compile("\"cfb2h\":\\s*\"([^\"]+)\"")
        private val SID_REGEX = Pattern.compile("\"FdrFJe\":\\s*\"([^\"]+)\"")
        private val ARTIFACTS_REGEX = Pattern.compile("https?://googleusercontent\\.com/(?:\\w+/)+\\d+\\n*")
    }

    private var cachedAccessToken: String? = null
    private var cachedBuildLabel: String? = null
    private var cachedSessionId: String? = null

    override fun models() = listOf(
        ModelInfo("account-default", "Gemini (your account default)"),
    )

    override suspend fun healthCheck(): Health {
        val psid = secrets.getRaw("GEMINI_PSID")
        return if (psid.isNullOrBlank()) {
            Health(false, "Gemini session cookies missing. Add them in Keys tab.")
        } else {
            Health(true, "Session cookies configured")
        }
    }

    private fun buildCookieHeader(): String {
        val psid = secrets.getRaw("GEMINI_PSID") ?: ""
        val psidts = secrets.getRaw("GEMINI_PSIDTS") ?: ""
        val sapisid = secrets.getRaw("GEMINI_SAPISID")
        val sb = StringBuilder()
        sb.append("__Secure-1PSID=").append(psid)
        if (psidts.isNotBlank()) sb.append("; __Secure-1PSIDTS=").append(psidts)
        if (!sapisid.isNullOrBlank()) sb.append("; SAPISID=").append(sapisid)
        return sb.toString()
    }

    private fun ensureTokens(): Boolean {
        if (!cachedAccessToken.isNullOrBlank()) return true
        val cookieHeader = buildCookieHeader()
        val req = Request.Builder()
            .url("https://gemini.google.com/app")
            .header("User-Agent", UA)
            .header("Cookie", cookieHeader)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val html = resp.body?.string().orEmpty()
                val snlm = SNLM_REGEX.matcher(html)
                if (snlm.find()) cachedAccessToken = snlm.group(1)
                val bl = BL_REGEX.matcher(html)
                if (bl.find()) cachedBuildLabel = bl.group(1)
                val sid = SID_REGEX.matcher(html)
                if (sid.find()) cachedSessionId = sid.group(1)
            }
        } catch (e: Exception) {
            return false
        }
        return !cachedAccessToken.isNullOrBlank()
    }

    override fun streamChat(request: ChatRequest): Flow<StreamEvent> = flow {
        val psid = secrets.getRaw("GEMINI_PSID")
        if (psid.isNullOrBlank()) {
            emit(StreamEvent.Error("Gemini Pro: session cookies not configured. Open Keys tab to log in.", false))
            return@flow
        }

        val start = System.currentTimeMillis()
        if (!ensureTokens()) {
            emit(StreamEvent.Error("Gemini Pro: session expired or unable to scrape tokens. Please update cookies in Keys tab.", true))
            return@flow
        }

        val prompt = if (request.history.size > 1) {
            request.history.dropLast(1).joinToString("\n") { m ->
                "${if (m.role == "user") "Human" else "Assistant"}: ${m.text}"
            } + "\nHuman: ${request.history.last().text}"
        } else {
            request.history.lastOrNull()?.text.orEmpty()
        }

        val reqId = (10000..99999).random()
        var genUrl = "https://gemini.google.com/_/BardChatUi/data/assistant.lamda.BardFrontendService/StreamGenerate?hl=en&_reqid=$reqId&rt=c"
        if (!cachedBuildLabel.isNullOrBlank()) genUrl += "&bl=$cachedBuildLabel"
        if (!cachedSessionId.isNullOrBlank()) genUrl += "&f.sid=$cachedSessionId"

        val uuidVal = UUID.randomUUID().toString().uppercase()

        // Construct 82-element JSPB envelope (safeguards indices 0..81)
        val innerList = Array(82) { "null" }
        innerList[0] = """[${Json.encodeToString(prompt)},0,null,null,null,null,0]"""
        innerList[1] = """["en"]"""
        innerList[2] = """["","","",null,null,null,null,null,null,""]"""
        innerList[6] = "[1]"
        innerList[7] = "1"
        innerList[10] = "1"
        innerList[11] = "0"
        innerList[17] = "[[0]]"
        innerList[18] = "0"
        innerList[27] = "1"
        innerList[30] = "[4]"
        innerList[41] = "[1]"
        innerList[53] = "0"
        innerList[59] = "\"$uuidVal\""
        innerList[61] = "[]"
        innerList[68] = "1"
        innerList[79] = "1"
        innerList[80] = "1"

        val innerJson = "[" + innerList.joinToString(",") + "]"
        val fReq = "[null,${Json.encodeToString(innerJson)}]"

        val formBody = FormBody.Builder()
            .add("at", cachedAccessToken.orEmpty())
            .add("f.req", fReq)
            .build()

        val reqBuilder = Request.Builder()
            .url(genUrl)
            .header("User-Agent", UA)
            .header("Origin", "https://gemini.google.com")
            .header("Referer", "https://gemini.google.com/")
            .header("X-Same-Domain", "1")
            .header("Cookie", buildCookieHeader())
            .header("x-goog-ext-525005358-jspb", "[\"$uuidVal\",1]")
            .post(formBody)

        val fullAnswer = StringBuilder()
        var lastEmittedLen = 0

        try {
            http.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    if (resp.code == 401 || resp.code == 403) {
                        cachedAccessToken = null
                    }
                    val err = resp.body?.string()?.take(300)
                    emit(StreamEvent.Error("Gemini Pro HTTP ${resp.code}: $err", resp.code in 429..599))
                    return@flow
                }

                val source = resp.body?.source() ?: run {
                    emit(StreamEvent.Error("Gemini Pro: response body is empty", true))
                    return@flow
                }

                while (!source.exhausted()) {
                    val rawLine = source.readUtf8Line() ?: break
                    val line = rawLine.trim()
                    if (line.isEmpty() || line.startsWith(")]}'") || line.all { it.isDigit() }) {
                        continue
                    }

                    if (line.startsWith("[[\"wrb.fr\"")) {
                        runCatching {
                            val outerArr = Json.parseToJsonElement(line).jsonArray
                            for (item in outerArr) {
                                val itemArr = item.jsonArray
                                val innerJsonStr = itemArr.getOrNull(2)?.jsonPrimitive?.content ?: continue
                                val partJson = Json.parseToJsonElement(innerJsonStr).jsonArray
                                val candidatesList = partJson.getOrNull(4)?.jsonArray ?: continue
                                if (candidatesList.isNotEmpty()) {
                                    val candidate = candidatesList[0].jsonArray
                                    val textArr = candidate.getOrNull(1)?.jsonArray
                                    val rawText = textArr?.getOrNull(0)?.jsonPrimitive?.content ?: continue
                                    val cleanedText = ARTIFACTS_REGEX.matcher(rawText).replaceAll("")
                                    if (cleanedText.length > lastEmittedLen) {
                                        val delta = cleanedText.substring(lastEmittedLen)
                                        lastEmittedLen = cleanedText.length
                                        fullAnswer.append(delta)
                                        emit(StreamEvent.Token(delta))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (fullAnswer.isEmpty()) {
                emit(StreamEvent.Error("Gemini Pro: empty response received from session stream", true))
                return@flow
            }

            emit(StreamEvent.Done(fullAnswer.toString(), System.currentTimeMillis() - start))
        } catch (e: Exception) {
            emit(StreamEvent.Error("Gemini Pro: ${e.message ?: e.javaClass.simpleName}", true))
        }
    }.flowOn(Dispatchers.IO)
}
