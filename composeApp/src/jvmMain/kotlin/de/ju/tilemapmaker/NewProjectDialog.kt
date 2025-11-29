package de.ju.tilemapmaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    // Callback jetzt mit Name, Breite, Höhe
    onCreate: (String, Int, Int) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    // Standardwerte für das Grid (als String für einfachere Eingabe)
    var gridWidth by remember { mutableStateOf("32") }
    var gridHeight by remember { mutableStateOf("32") }

    // Wiederverwendbare Farben für Konsistenz
    val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = Color.White,
        cursorColor = Color.White,
        focusedBorderColor = Color(0xFF589DF6),
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = Color(0xFF589DF6),
        unfocusedLabelColor = Color.Gray
    )

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        MaterialTheme(colors = darkColors()) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .wrapContentHeight()
                    .background(Color(0xFF3C3F41))
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Create New Project",
                    style = MaterialTheme.typography.h6,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Name Input
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Grid Inputs (Nebeneinander)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // WIDTH
                    OutlinedTextField(
                        value = gridWidth,
                        onValueChange = { input ->
                            // Nur Ziffern erlauben
                            if (input.all { it.isDigit() }) {
                                gridWidth = input
                            }
                        },
                        label = { Text("Width") },
                        singleLine = true,
                        modifier = Modifier.weight(1f), // Nimmt 50% Platz
                        colors = textFieldColors
                    )

                    // HEIGHT
                    OutlinedTextField(
                        value = gridHeight,
                        onValueChange = { input ->
                            // Nur Ziffern erlauben
                            if (input.all { it.isDigit() }) {
                                gridHeight = input
                            }
                        },
                        label = { Text("Height") },
                        singleLine = true,
                        modifier = Modifier.weight(1f), // Nimmt 50% Platz
                        colors = textFieldColors
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }

                    // Button ist nur aktiv, wenn alles ausgefüllt ist
                    val isFormValid = projectName.isNotBlank() &&
                            gridWidth.isNotBlank() &&
                            gridHeight.isNotBlank()

                    Button(
                        onClick = {
                            if (isFormValid) {
                                // Strings zu Int konvertieren und zurückgeben
                                onCreate(
                                    projectName,
                                    gridWidth.toIntOrNull() ?: 32,
                                    gridHeight.toIntOrNull() ?: 32
                                )
                            }
                        },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF365880)),
                        enabled = isFormValid
                    ) {
                        Text("Create", color = Color.White)
                    }
                }
            }
        }
    }
}