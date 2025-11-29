package de.ju.tilemapmaker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo / Titel
        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            tint = Color(0xFF589DF6),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tile Map Maker",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Create a new map or open an existing project",
            style = MaterialTheme.typography.body1,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Die Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionButton(
                text = "New Project",
                icon = Icons.Default.Add,
                onClick = onNewProject
            )

            ActionButton(
                text = "Open",
                icon = Icons.Default.FolderOpen,
                onClick = onOpenProject,
                isPrimary = false
            )
        }
    }
}
