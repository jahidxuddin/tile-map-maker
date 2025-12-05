package de.ju.tilemapmaker

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

@Serializable
data class LayerConfig(
    val name: String,
    val isVisible: Boolean = true,
    val tiles: Map<String, String> = emptyMap()
)

@Serializable
data class ProjectConfig(
    val name: String,
    val width: Int,
    val height: Int,
    val path: String? = null,
    // Neue Struktur: Liste von Layern
    val layers: List<LayerConfig> = listOf(LayerConfig("Background")),
    // Für Migration: Alte Projekte landen hier
    @SerialName("tiles") val oldTiles: Map<String, String>? = null
)

fun readProjectConfig(folderPath: String): ProjectConfig? {
    val configFile = File(folderPath, "config.json")
    if (!configFile.exists()) return null
    return try {
        val jsonString = configFile.readText()
        // isLenient und ignoreUnknownKeys helfen bei Versionsunterschieden
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true; isLenient = true }
        json.decodeFromString<ProjectConfig>(jsonString)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveProjectConfig(config: ProjectConfig): Boolean {
    val path = config.path ?: return false
    return try {
        // Beim Speichern entfernen wir Altlasten (oldTiles), da wir jetzt layers nutzen
        val cleanConfig = config.copy(oldTiles = null)
        val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
        val jsonString = json.encodeToString(cleanConfig)
        File(path, "config.json").writeText(jsonString)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun createProjectConfig(config: ProjectConfig): Boolean {
    val parentPath = config.path ?: return false
    return try {
        val projectDir = File(parentPath, config.name)
        if (!projectDir.exists() && !projectDir.mkdirs()) return false

        // Pfad auf den neu erstellten Unterordner aktualisieren
        val finalConfig = config.copy(path = projectDir.absolutePath)
        saveProjectConfig(finalConfig)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun main() = androidx.compose.ui.window.application {
    var activeProject by remember { mutableStateOf<ProjectConfig?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }

    if (activeProject == null) {
        WelcomeWindow(
            showErrorDialog = showErrorDialog,
            onDismissError = { showErrorDialog = false },
            onOpenProject = { path ->
                val config = readProjectConfig(path)
                if (config != null) activeProject = config.copy(path = path) else showErrorDialog = true
            },
            onCreateProject = { name, width, height ->
                val parentPath = openFolderDialog(null)
                if (parentPath != null) {
                    val tempConfig = ProjectConfig(name, width, height, path = parentPath)
                    if (createProjectConfig(tempConfig)) {
                        activeProject = tempConfig.copy(path = File(parentPath, name).absolutePath)
                    } else showErrorDialog = true
                }
            },
            onExit = ::exitApplication
        )
    } else {
        key(activeProject) {
            EditorWindow(
                project = activeProject!!,
                onClose = ::exitApplication,
                onCloseProject = { activeProject = null },
                onProjectChange = { newConfig -> activeProject = newConfig }
            )
        }
    }
}

fun openFolderDialog(window: ComposeWindow?): String? {
    val fileChooser = JFileChooser()
    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    fileChooser.dialogTitle = "Select Project Folder"
    val result = fileChooser.showOpenDialog(window)
    return if (result == JFileChooser.APPROVE_OPTION) fileChooser.selectedFile.absolutePath else null
}