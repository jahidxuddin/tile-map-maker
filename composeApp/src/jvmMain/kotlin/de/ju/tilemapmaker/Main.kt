package de.ju.tilemapmaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(width = 800.dp, height = 600.dp, position = WindowPosition.Aligned(Alignment.Center))

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        undecorated = true,
        transparent = false,

    ) {
        MaterialTheme(colors = darkColors()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- TITLE BAR ---
                CustomTitleBar(
                    windowState = windowState,
                    onClose = ::exitApplication,
                    windowScope = this@Window
                )

                // --- MAIN CONTENT AREA ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2B2D30)),
                    contentAlignment = Alignment.Center
                ) {
                    WelcomeScreen(
                        onNewProject = { /* TODO: Logik für Neu */ },
                        onOpenProject = { /* TODO: Logik für Öffnen */ }
                    )
                }
            }
        }
    }
}
