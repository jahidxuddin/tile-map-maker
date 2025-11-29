package de.ju.tilemapmaker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState

@Composable
fun CustomTitleBar(
    windowState: WindowState,
    onClose: () -> Unit,
    windowScope: WindowScope,
    triggerOpenProject: () -> Unit,
    triggerNewProject: () -> Unit,
) {
    var fileMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp).background(Color(0xFF3C3F41)),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Linke Seite (Drag Area + Menü)
        windowScope.WindowDraggableArea(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("🛠️", modifier = Modifier.padding(end = 8.dp))

                // File Menü
                Box {
                    Text(
                        text = "File",
                        color = Color.LightGray,
                        modifier = Modifier.clickable { fileMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp))

                    DropdownMenu(
                        expanded = fileMenuExpanded, onDismissRequest = { fileMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF3C3F41))
                    ) {

                        // 1. Button: New Project
                        DropdownMenuItem(onClick = {
                            fileMenuExpanded = false
                            triggerNewProject()
                        }) {
                            Text("New Project", color = Color.White)
                        }

                        // 2. Button: Open
                        DropdownMenuItem(onClick = {
                            fileMenuExpanded = false
                            triggerOpenProject()
                        }) {
                            Text("Open...", color = Color.White)
                        }

                        Divider(color = Color.Gray)

                        // 3. Button: Exit
                        DropdownMenuItem(onClick = {
                            fileMenuExpanded = false
                            onClose() // <--- Schließt die App
                        }) {
                            Text("Exit", color = Color.White)
                        }
                    }
                }
            }
        }

        // Rechte Seite (Fenster Kontrollen)
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Minimieren
            IconButton(
                onClick = { windowState.isMinimized = true }, modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Minimize,
                    contentDescription = "Minimize",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Maximieren / Restore
            IconButton(
                onClick = {
                    windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Maximized
                    }
                }, modifier = Modifier.size(32.dp)
            ) {
                val icon = if (windowState.placement == WindowPlacement.Maximized) {
                    Icons.Default.FilterNone
                } else {
                    Icons.Default.CropSquare
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Maximize",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Schließen
            IconButton(
                onClick = onClose, modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}