package de.ju.tilemapmaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import javax.swing.JFileChooser
import javax.swing.UIManager

fun main() = application {
    val windowState =
        rememberWindowState(width = 800.dp, height = 600.dp, position = WindowPosition.Aligned(Alignment.Center))

    var showNewProjectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        undecorated = true,
        transparent = false,
        title = "Tile Map Maker",
    ) {
        val triggerNewProject = { showNewProjectDialog = true }
        val triggerOpenProject = {
            // 'this.window' ist hier verfügbar durch den WindowScope
            val path = openFolderDialog(window)

            if (path != null) {
                println("Ordner ausgewählt: $path")
                // TODO: Hier Projekt laden
            }
        }

        MaterialTheme(colors = darkColors()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- TITLE BAR ---
                CustomTitleBar(
                    windowState = windowState, onClose = ::exitApplication, windowScope = this@Window, triggerOpenProject = triggerOpenProject, triggerNewProject = triggerNewProject
                )

                // --- MAIN CONTENT AREA ---
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF2B2D30)), contentAlignment = Alignment.Center
                ) {
                    WelcomeScreen(onNewProject = triggerNewProject, onOpenProject = triggerOpenProject)
                }

                if (showNewProjectDialog) {
                    NewProjectDialog(onDismiss = { showNewProjectDialog = false }, onCreate = { name ->
                        println("Project '$name' created!")
                        showNewProjectDialog = false
                    })
                }
            }
        }
    }
}

fun openFolderDialog(window: ComposeWindow?): String? {
    val fileChooser = JFileChooser()

    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY

    fileChooser.dialogTitle = "Select Project Folder"

    val result = fileChooser.showOpenDialog(window)

    return if (result == JFileChooser.APPROVE_OPTION) {
        fileChooser.selectedFile.absolutePath
    } else {
        null
    }
}