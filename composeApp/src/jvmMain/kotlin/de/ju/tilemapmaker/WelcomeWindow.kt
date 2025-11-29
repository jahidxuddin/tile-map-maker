package de.ju.tilemapmaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState

@Composable
fun ApplicationScope.WelcomeWindow(
    showErrorDialog: Boolean,
    onDismissError: () -> Unit,
    onOpenProject: (String) -> Unit,
    onCreateProject: (String, Int, Int) -> Unit,
    onExit: () -> Unit
) {
    val windowState = rememberWindowState(
        width = 800.dp, height = 600.dp, position = WindowPosition.Aligned(Alignment.Center)
    )
    var showNewProjectDialog by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = onExit,
        state = windowState,
        undecorated = true,
        transparent = false,
        title = "Tile Map Maker - Welcome",
    ) {
        val triggerNewProject = { showNewProjectDialog = true }

        val triggerOpenProject = {
            val path = openFolderDialog(window)
            if (path != null) onOpenProject(path)
        }

        MaterialTheme(colors = darkColors()) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTitleBar(
                    windowState = windowState,
                    onClose = onExit,
                    windowScope = this@Window,
                    triggerOpenProject = triggerOpenProject,
                    triggerNewProject = triggerNewProject,
                    showDropdownMenu = false
                )

                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF2B2D30)), contentAlignment = Alignment.Center
                ) {
                    WelcomeScreen(onNewProject = triggerNewProject, onOpenProject = triggerOpenProject)
                }

                if (showErrorDialog) {
                    ErrorDialog(
                        message = "Das Projekt konnte nicht geladen oder erstellt werden.", onDismiss = onDismissError
                    )
                }

                if (showNewProjectDialog) {
                    NewProjectDialog(onDismiss = { showNewProjectDialog = false }, onCreate = { name, w, h ->
                        showNewProjectDialog = false
                        onCreateProject(name, w, h)
                    })
                }
            }
        }
    }
}