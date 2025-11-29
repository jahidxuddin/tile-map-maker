package de.ju.tilemapmaker

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fehler", color = Color.White) },
        text = { Text(message, color = Color.LightGray) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = Color(0xFF589DF6)) // Blau
            }
        },
        backgroundColor = Color(0xFF3C3F41), // Dunkler Hintergrund
        contentColor = Color.White
    )
}