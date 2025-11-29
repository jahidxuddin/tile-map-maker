package de.ju.tilemapmaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication, state = windowState,
        undecorated = true, transparent = false
    ) {
        MaterialTheme(colors = darkColors()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 2. Deine Custom Title Bar
                CustomTitleBar(
                    windowState = windowState,
                    onClose = ::exitApplication, windowScope = this@Window
                )

                // 3. Der eigentliche App-Inhalt
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF2B2D30)).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Hier ist dein Editor-Bereich", color = Color.White)
                }
            }
        }
    }
}

