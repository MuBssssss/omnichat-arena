package com.omnichat.arena.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnichat.arena.core.AiProvider
import com.omnichat.arena.core.ChatMessage
import com.omnichat.arena.core.ChatRequest
import com.omnichat.arena.core.ProviderId
import com.omnichat.arena.core.selectable
import com.omnichat.arena.core.StreamEvent
import com.omnichat.arena.data.ChatDao
import com.omnichat.arena.data.ConversationEntity
import com.omnichat.arena.data.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatLine(val who: String, val text: String, val streaming: Boolean = false)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val providers: Map<String, @JvmSuppressWildcards AiProvider>,
    private val chatDao: ChatDao,
) : ViewModel() {
    private val _lines = MutableStateFlow<List<ChatLine>>(emptyList())
    val lines: StateFlow<List<ChatLine>> = _lines.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    var selected: String by mutableStateOf("FAKE"); private set
    fun providerIds(): List<String> =
        providers.keys.filter { ProviderId.valueOf(it).selectable }.sorted()

    private var currentConversationId: String? = null
    private var job: Job? = null

    init {
        loadMostRecentConversation()
    }

    private fun loadMostRecentConversation() {
        viewModelScope.launch {
            val convs = chatDao.observeConversations().firstOrNull().orEmpty()
            val solo = convs.firstOrNull { it.mode == "SOLO" }
            if (solo != null) {
                if (providers.containsKey(solo.providerIds)) {
                    selected = solo.providerIds
                }
                loadConversation(solo.id)
            }
        }
    }

    fun select(id: String) {
        if (selected == id) return
        selected = id
        job?.cancel()
        _busy.value = false
        viewModelScope.launch {
            val convs = chatDao.observeConversations().firstOrNull().orEmpty()
            val existing = convs.firstOrNull { it.mode == "SOLO" && it.providerIds == id }
            if (existing != null) {
                loadConversation(existing.id)
            } else {
                currentConversationId = null
                _lines.value = emptyList()
            }
        }
    }

    private suspend fun loadConversation(cid: String) {
        currentConversationId = cid
        val msgs = chatDao.getMessages(cid)
        _lines.value = msgs.map { m ->
            if (m.role == "USER") {
                ChatLine("You", m.text)
            } else {
                val name = runCatching {
                    ProviderId.valueOf(m.providerId.orEmpty()).displayName
                }.getOrDefault(m.providerId ?: "AI")
                if (m.status == "ERROR") {
                    ChatLine(name, "⚠️ ${m.text}")
                } else {
                    ChatLine(name, m.text)
                }
            }
        }
    }

    fun send(prompt: String) {
        if (prompt.isBlank() || _busy.value) return
        val provider = providers[selected] ?: return
        job?.cancel()
        _lines.value = _lines.value + ChatLine("You", prompt)
        _busy.value = true
        job = viewModelScope.launch {
            val acc = StringBuilder()
            val pastHistory = _lines.value
                .filterNot { it.streaming }
                .dropLast(1) // exclude just added current prompt
                .map { line ->
                    if (line.who == "You") ChatMessage.user(line.text)
                    else ChatMessage.model(line.text.removePrefix("⚠️ "))
                }
            val req = ChatRequest(history = pastHistory + ChatMessage.user(prompt))
            provider.streamChat(req).collect { ev ->
                when (ev) {
                    is StreamEvent.Token -> {
                        acc.append(ev.delta)
                        _lines.value = _lines.value
                            .filterNot { it.streaming } + ChatLine(provider.id.displayName, acc.toString(), true)
                    }
                    is StreamEvent.Done -> {
                        _lines.value = _lines.value
                            .filterNot { it.streaming } + ChatLine(provider.id.displayName, ev.fullText)
                        _busy.value = false
                        persistExchange(prompt, ev.fullText, ev.latencyMs, status = "DONE")
                    }
                    is StreamEvent.Error -> {
                        _lines.value = _lines.value
                            .filterNot { it.streaming } + ChatLine(provider.id.displayName, "⚠️ ${ev.message}")
                        _busy.value = false
                        persistExchange(prompt, ev.message, latencyMs = null, status = "ERROR")
                    }
                }
            }
        }
    }

    private suspend fun persistExchange(
        prompt: String,
        responseText: String,
        latencyMs: Long?,
        status: String,
    ) {
        val now = System.currentTimeMillis()
        val convs = chatDao.observeConversations().firstOrNull().orEmpty()
        val existing = convs.firstOrNull { it.mode == "SOLO" && it.providerIds == selected }
        val cid = existing?.id ?: currentConversationId ?: UUID.randomUUID().toString()
        currentConversationId = cid

        if (existing != null) {
            chatDao.upsertConversation(existing.copy(updatedAt = now))
        } else {
            val title = prompt.take(40)
            chatDao.upsertConversation(
                ConversationEntity(
                    id = cid,
                    title = title,
                    providerIds = selected,
                    mode = "SOLO",
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }

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
                createdAt = now - (latencyMs ?: 0L),
            )
        )

        chatDao.insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = cid,
                role = "AI",
                providerId = selected,
                modelName = providers[selected]?.models()?.firstOrNull()?.id,
                text = responseText,
                latencyMs = latencyMs,
                status = status,
                createdAt = now,
            )
        )
    }
}

@Composable
fun ChatScreen(mod: Modifier = Modifier, vm: ChatViewModel = hiltViewModel()) {
    val lines by vm.lines.collectAsState()
    val busy by vm.busy.collectAsState()
    var input by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, lines.lastOrNull()?.text?.length) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    fun sendPrompt() {
        val prompt = input.trim()
        if (prompt.isNotEmpty() && !busy) {
            vm.send(prompt)
            input = ""
        }
    }

    Column(mod.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { menu = true }) {
                Text(ProviderId.valueOf(vm.selected).displayName)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                vm.providerIds().forEach { id ->
                    DropdownMenuItem(
                        text = { Text(ProviderId.valueOf(id).displayName) },
                        onClick = { vm.select(id); menu = false }
                    )
                }
            }
            Text(
                text = if (busy) "…streaming" else "Ready",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.labelMedium,
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(lines) { line ->
                val isUser = line.who == "You"
                val isError = line.text.startsWith("⚠️")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isError -> MaterialTheme.colorScheme.errorContainer
                                isUser -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    ) {
                        Text("${line.who}:\n${line.text}", Modifier.padding(10.dp))
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendPrompt() }),
            )
            Button(onClick = { sendPrompt() }, enabled = !busy && input.isNotBlank()) { Text("Send") }
        }
    }
}
