package com.omnichat.arena.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnichat.arena.core.*
import com.omnichat.arena.data.ChatDao
import com.omnichat.arena.data.ConversationEntity
import com.omnichat.arena.data.MessageEntity
import com.omnichat.arena.data.SecretStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val providers: Map<String, @JvmSuppressWildcards AiProvider>,
    private val judge: JudgeEngine,
    private val chatDao: ChatDao,
    private val secrets: SecretStore,
) : ViewModel() {

    private val _picked = MutableStateFlow<Set<String>>(setOf("FAKE", "POLLINATIONS"))
    val picked: StateFlow<Set<String>> = _picked.asStateFlow()

    private val _answers = MutableStateFlow<Map<String, String>>(emptyMap())
    val answers: StateFlow<Map<String, String>> = _answers.asStateFlow()

    private val _verdict = MutableStateFlow<JudgeVerdict?>(null)
    val verdict: StateFlow<JudgeVerdict?> = _verdict.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun providerIds(): List<String> = providers.keys
        .filter { idStr ->
            runCatching { ProviderId.valueOf(idStr).selectable }.getOrDefault(false)
        }
        .sorted()

    fun toggle(id: String) {
        val cur = _picked.value.toMutableSet()
        if (id in cur) {
            if (cur.size > 1) cur.remove(id)
        } else {
            cur.add(id)
        }
        _picked.value = cur
    }

    fun runArena(prompt: String) {
        if (prompt.isBlank() || _busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _answers.value = _picked.value.associateWith { "" }
            _verdict.value = null

            val chosen = _picked.value.mapNotNull { providers[it] }
            val req = ChatRequest(listOf(ChatMessage.user(prompt)))

            val results = kotlinx.coroutines.coroutineScope {
                chosen.map { p ->
                    async {
                        val id = p.id.name
                        val t0 = System.currentTimeMillis()
                        val sb = StringBuilder()
                        var err: String? = null
                        try {
                            p.streamChat(req).collect { ev ->
                                when (ev) {
                                    is StreamEvent.Token -> {
                                        sb.append(ev.delta)
                                        _answers.value = _answers.value + (id to sb.toString())
                                    }
                                    is StreamEvent.Error -> err = ev.message
                                    is StreamEvent.Done -> Unit
                                }
                            }
                        } catch (e: Exception) {
                            err = e.message ?: "unknown"
                        }
                        val ms = System.currentTimeMillis() - t0
                        if (err != null) _answers.value = _answers.value + (id to "⚠️ $err")
                        ArenaAnswer(p.id, p.models().firstOrNull()?.label ?: id, sb.toString(), ms, err)
                    }
                }.awaitAll().filterNotNull()
            }
            val fastVerdict = judge.fastJudge(prompt, results)
            _verdict.value = fastVerdict

            // T4: Groq-as-judge (anonymized + shuffled evaluation + fused best answer)
            val groq = providers["GROQ"]
            val hasGroq = secrets.has(ProviderId.GROQ)

            val finalVerdict = if (groq != null && hasGroq) {
                val prepared = judge.buildJudgePrompt(prompt, results)
                if (prepared != null) {
                    val (judgePrompt, letterMap) = prepared
                    try {
                        val judgeSb = StringBuilder()
                        var judgeErr: String? = null
                        groq.streamChat(ChatRequest(listOf(ChatMessage.user(judgePrompt)))).collect { ev ->
                            when (ev) {
                                is StreamEvent.Token -> judgeSb.append(ev.delta)
                                is StreamEvent.Error -> judgeErr = ev.message
                                is StreamEvent.Done -> Unit
                            }
                        }
                        if (judgeErr != null) {
                            fastVerdict.copy(rationale = "${fastVerdict.rationale} (LLM judge error: $judgeErr)")
                        } else {
                            judge.parseLlmVerdict(judgeSb.toString(), letterMap, fastVerdict)
                        }
                    } catch (e: Exception) {
                        fastVerdict.copy(rationale = "${fastVerdict.rationale} (LLM judge unavailable: ${e.message ?: "error"})")
                    }
                } else {
                    fastVerdict
                }
            } else {
                fastVerdict.copy(rationale = "${fastVerdict.rationale} (LLM judge unavailable — add Groq key in Keys tab)")
            }

            _verdict.value = finalVerdict
            _busy.value = false
            persistArenaRun(prompt, results, finalVerdict)
        }
    }

    private suspend fun persistArenaRun(
        prompt: String,
        answers: List<ArenaAnswer>,
        verdictObj: JudgeVerdict?,
    ) {
        val now = System.currentTimeMillis()
        val cid = UUID.randomUUID().toString()
        val providerIdsStr = _picked.value.sorted().joinToString(",")
        val title = prompt.take(40)
        val conv = ConversationEntity(
            id = cid,
            title = title,
            providerIds = providerIdsStr,
            mode = "ARENA",
            createdAt = now,
            updatedAt = now,
        )
        chatDao.upsertConversation(conv)

        // User prompt
        chatDao.insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = cid,
                role = "USER",
                providerId = null,
                modelName = null,
                text = prompt,
                latencyMs = null,
                status = "DONE",
                createdAt = now,
            )
        )

        // Contenders
        answers.forEach { a ->
            val hasError = a.error != null
            chatDao.insertMessage(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = cid,
                    role = "AI",
                    providerId = a.providerId.name,
                    modelName = a.modelName,
                    text = a.error ?: a.text,
                    latencyMs = a.latencyMs,
                    status = if (hasError) "ERROR" else "DONE",
                    createdAt = now + a.latencyMs,
                )
            )
        }

        // Verdict
        if (verdictObj != null) {
            val scoresStr = verdictObj.scores.joinToString(", ") {
                val llmTag = if (it.llmScore != null) " (LLM)" else ""
                "${it.providerId.displayName}: ${it.score}$llmTag"
            }
            var verdictText = "🏆 Winner: ${verdictObj.winner.displayName}\nScores: $scoresStr\nRationale: ${verdictObj.rationale}"
            if (!verdictObj.fusedAnswer.isNullOrBlank()) {
                verdictText += "\n\n✨ Fused Best Answer:\n${verdictObj.fusedAnswer}"
            }
            chatDao.insertMessage(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = cid,
                    role = "VERDICT",
                    providerId = null,
                    modelName = null,
                    text = verdictText,
                    latencyMs = null,
                    status = "DONE",
                    createdAt = now + 1,
                )
            )
        }
    }
}

@Composable
fun CompareScreen(mod: Modifier = Modifier, vm: CompareViewModel = hiltViewModel()) {
    val picked by vm.picked.collectAsState()
    val answers by vm.answers.collectAsState()
    val verdict by vm.verdict.collectAsState()
    val busy by vm.busy.collectAsState()
    var input by remember { mutableStateOf("") }
    Column(mod.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pick contenders:", style = MaterialTheme.typography.titleSmall)
        // LazyRow: chips scroll horizontally instead of squeezing into vertical text.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(vm.providerIds()) { id ->
                FilterChip(
                    selected = id in picked, onClick = { vm.toggle(id) },
                    label = { Text(ProviderId.valueOf(id).displayName) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f), placeholder = { Text("One prompt, N AIs…") }
            )
            Button(onClick = { vm.runArena(input) }, enabled = !busy) { Text("Arena!") }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            verdict?.let { v ->
                item {
                    var showFused by remember(v) { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🏆 Winner: ${v.winner.displayName}", style = MaterialTheme.typography.titleMedium)
                            v.scores.forEach { s ->
                                val llmTag = if (s.llmScore != null) " (LLM)" else ""
                                Text("${s.providerId.displayName}: ${s.score}$llmTag")
                            }
                            Text(v.rationale, style = MaterialTheme.typography.bodySmall)

                            if (!v.fusedAnswer.isNullOrBlank()) {
                                Button(
                                    onClick = { showFused = !showFused },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(if (showFused) "Hide fused answer" else "✨ Show fused best answer")
                                }
                                if (showFused) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            Text(
                                                "✨ Fused Best Answer (synthesized by Judge):",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Text(v.fusedAnswer, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            items(answers.entries.toList()) { (id, text) ->
                val isWinner = verdict?.winner?.name == id
                Card(
                    colors = if (isWinner) CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) else CardDefaults.cardColors()
                ) {
                    Text(
                        "${if (isWinner) "🏆 " else ""}${ProviderId.valueOf(id).displayName}:\n$text",
                        Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
