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
    onDismiss: () -> Unit, onCreate: (String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        MaterialTheme(colors = darkColors()) {
            Column(
                modifier = Modifier.width(400.dp)
                    .wrapContentHeight()
                    .background(Color(0xFF3C3F41)).padding(16.dp), verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Create New Project", style = MaterialTheme.typography.h6, color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color(0xFF589DF6),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF589DF6),
                        unfocusedLabelColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss, modifier = Modifier.padding(end = 8.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            if (projectName.isNotBlank()) {
                                onCreate(projectName)
                            }
                        },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF365880)),
                        enabled = projectName.isNotBlank()
                    ) {
                        Text("Create", color = Color.White)
                    }
                }
            }
        }
    }
}