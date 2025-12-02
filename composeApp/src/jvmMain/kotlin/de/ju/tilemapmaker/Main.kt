package de.ju.tilemapmaker

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

@Serializable
data class ProjectConfig(
    val name: String,
    val width: Int,
    val height: Int,
    val path: String? = null,
    val tiles: Map<String, String> = emptyMap()
)

fun readProjectConfig(folderPath: String): ProjectConfig? {
    val configFile = File(folderPath, "config.json")
    if (!configFile.exists()) {
        println("Keine config.json in $folderPath gefunden.")
        return null
    }
    return try {
        val jsonString = configFile.readText()
        val jsonParser = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }
        jsonParser.decodeFromString<ProjectConfig>(jsonString)
    } catch (e: Exception) {
        println("Fehler beim Lesen der config.json: ${e.message}")
        null
    }
}

fun saveProjectConfig(config: ProjectConfig): Boolean {
    val path = config.path ?: return false
    return try {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val jsonString = json.encodeToString(config)
        File(path, "config.json").writeText(jsonString)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun createProjectConfig(config: ProjectConfig): Boolean {
    val parentPath = config.path
    if (parentPath.isNullOrBlank()) return false

    return try {
        val projectDir = File(parentPath, config.name)
        if (!projectDir.exists()) {
            if (!projectDir.mkdirs()) return false
        }

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
                if (config != null) {
                    activeProject = config.copy(path = path)
                } else {
                    showErrorDialog = true
                }
            },
            onCreateProject = { name, width, height ->
                val parentPath = openFolderDialog(null)
                if (parentPath != null) {
                    val tempConfig = ProjectConfig(name, width, height, path = parentPath)
                    if (createProjectConfig(tempConfig)) {
                        val fullPath = File(parentPath, name).absolutePath
                        activeProject = tempConfig.copy(path = fullPath)
                    } else {
                        showErrorDialog = true
                    }
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