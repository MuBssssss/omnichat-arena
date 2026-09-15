package com.omnichat.arena.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.omnichat.arena.core.ProviderId
import com.omnichat.arena.data.SecretStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secrets: SecretStore,
) : ViewModel() {
    fun has(p: ProviderId) = secrets.has(p)
    fun save(p: ProviderId, v: String) = secrets.set(p, v)
    fun clear(p: ProviderId) = secrets.clear(p)

    fun hasGeminiSession(): Boolean = !secrets.getRaw("GEMINI_PSID").isNullOrBlank()
    fun saveGeminiSession(psid: String, psidts: String) {
        if (psid.isNotBlank()) secrets.setRaw("GEMINI_PSID", psid.trim())
        if (psidts.isNotBlank()) secrets.setRaw("GEMINI_PSIDTS", psidts.trim())
    }
    fun clearGeminiSession() {
        secrets.clearRaw("GEMINI_PSID")
        secrets.clearRaw("GEMINI_PSIDTS")
        secrets.clearRaw("GEMINI_SAPISID")
    }

    fun hasPerplexitySession(): Boolean = !secrets.getRaw("PERPLEXITY_SESSION_TOKEN").isNullOrBlank()
    fun savePerplexitySession(token: String) {
        if (token.isNotBlank()) secrets.setRaw("PERPLEXITY_SESSION_TOKEN", token.trim())
    }
    fun clearPerplexitySession() {
        secrets.clearRaw("PERPLEXITY_SESSION_TOKEN")
    }
}

@Composable
fun SettingsScreen(mod: Modifier = Modifier, vm: SettingsViewModel = hiltViewModel()) {
    var tick by remember { mutableStateOf(0) } // refresh has() indicators
    Column(
        mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Keys & logins (stored encrypted, on-device only)", style = MaterialTheme.typography.titleMedium)
        GeminiSessionCard(vm, tick) { tick++ }
        PerplexitySessionCard(vm, tick) { tick++ }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text("DeepSeek — ⛔ bot-walled, parked")
                Text("AWS WAF JS-challenge + 144k PoW puzzle (Sep 2026 spike). Parked per $0 plan.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        KeyRow(ProviderId.GROQ, "FREE @ console.groq.com — also powers the $0 judge", vm, tick) { tick++ }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text("Pollinations — free, no key needed ✅")
                Text("Shared free models. Has spending caps, can flake — Groq is steadier.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text("Duck.ai — ⛔ bot-walled, parked")
                Text("JS challenge + 418 on every network (Sep 2026 spike). Auto-retry later.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text("Claude · Grok 🔜")
                Text("One provider file + one @Binds line each. See plan doc §9.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun GeminiSessionCard(vm: SettingsViewModel, tick: Int, bump: () -> Unit) {
    val ctx = LocalContext.current
    var psid by remember(tick) { mutableStateOf("") }
    var psidts by remember(tick) { mutableStateOf("") }
    val loggedIn = vm.hasGeminiSession()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Gemini Pro (Web Session) ${if (loggedIn) "✅ Logged in" else "⬜ Not logged in"}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (loggedIn) {
                    TextButton(onClick = { vm.clearGeminiSession(); bump() }) {
                        Text("Log out")
                    }
                }
            }
            Text(
                "Uses your Gemini Pro account with Pro reasoning models ($0-only).",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app"))
                        ctx.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Log in via Browser")
                }
            }
            OutlinedTextField(
                value = psid,
                onValueChange = { psid = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("__Secure-1PSID (starts with g.)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            OutlinedTextField(
                value = psidts,
                onValueChange = { psidts = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("__Secure-1PSIDTS") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Button(
                onClick = {
                    vm.saveGeminiSession(psid, psidts)
                    psid = ""
                    psidts = ""
                    bump()
                },
                enabled = psid.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Session Cookies")
            }
        }
    }
}

@Composable
private fun PerplexitySessionCard(vm: SettingsViewModel, tick: Int, bump: () -> Unit) {
    val ctx = LocalContext.current
    var token by remember(tick) { mutableStateOf("") }
    val loggedIn = vm.hasPerplexitySession()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Perplexity (Web Session) ${if (loggedIn) "✅ Logged in" else "⬜ Not logged in"}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (loggedIn) {
                    TextButton(onClick = { vm.clearPerplexitySession(); bump() }) {
                        Text("Log out")
                    }
                }
            }
            Text(
                "Unofficial web-session integration ($0-only). Requires __Secure-next-auth.session-token from your logged-in browser session. Never leaves device.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.perplexity.ai"))
                        ctx.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open Perplexity in Browser")
                }
            }
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("__Secure-next-auth.session-token") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Button(
                onClick = {
                    vm.savePerplexitySession(token)
                    token = ""
                    bump()
                },
                enabled = token.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Session Token")
            }
        }
    }
}

@Composable
private fun KeyRow(
    p: ProviderId, hint: String, vm: SettingsViewModel, tick: Int, bump: () -> Unit,
) {
    var v by remember(tick) { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${p.displayName} ${if (vm.has(p)) "✅" else "⬜"}")
                if (vm.has(p)) TextButton(onClick = { vm.clear(p); bump() }) { Text("Clear") }
            }
            Text(hint, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = v, onValueChange = { v = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("paste key") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                )
                Button(onClick = { vm.save(p, v); v = ""; bump() }, enabled = v.isNotBlank()) {
                    Text("Save")
                }
            }
        }
    }
}
