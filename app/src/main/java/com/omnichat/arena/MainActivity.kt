package com.omnichat.arena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import com.omnichat.arena.ui.ChatScreen
import com.omnichat.arena.ui.CompareScreen
import com.omnichat.arena.ui.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.material.icons.automirrored.filled.Chat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OmniRoot() }
    }
}

@Composable
fun OmniRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, null) }, label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.EmojiEvents, null) }, label = { Text("Arena") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Keys") }
                )
            }
        }
    ) { pad ->
        when (tab) {
            0 -> ChatScreen(Modifier.padding(pad))
            1 -> CompareScreen(Modifier.padding(pad))
            else -> SettingsScreen(Modifier.padding(pad))
        }
    }
}
