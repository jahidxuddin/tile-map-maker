package de.ju.tilemapmaker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.io.File
import javax.imageio.ImageIO

// Konstante für die Darstellung: Wie groß ist ein Tile im Editor in Pixeln?
const val TILE_SIZE_DP = 32

@Composable
fun ApplicationScope.EditorWindow(
    project: ProjectConfig, onClose: () -> Unit, onCloseProject: () -> Unit, onProjectChange: (ProjectConfig) -> Unit
) {
    val windowState =
        rememberWindowState(placement = WindowPlacement.Maximized, position = WindowPosition.Aligned(Alignment.Center))
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showLoadError by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = onClose,
        state = windowState,
        undecorated = true,
        title = "Tile Map Maker - ${project.name}",
    ) {
        MaterialTheme(colors = darkColors()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- TITLE BAR ---
                CustomTitleBar(
                    windowState = windowState,
                    onClose = onClose,
                    windowScope = this@Window,
                    triggerOpenProject = {
                        val path = openFolderDialog(window)
                        if (path != null) {
                            val config = readProjectConfig(path)
                            if (config != null) {
                                onProjectChange(config.copy(path = path))
                            } else {
                                showLoadError = true
                            }
                        }
                    },
                    triggerNewProject = { showNewProjectDialog = true },
                    showDropdownMenu = true // Parameter existiert laut vorherigem Kontext
                )

                // --- MAIN CONTENT (SPLIT VIEW) ---
                Row(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1F22))) {

                    // 1. LINKER BEREICH: MAP GRID
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        MapGridArea(project.width, project.height)
                    }

                    // Vertikale Trennlinie
                    Divider(
                        modifier = Modifier.fillMaxHeight().width(1.dp), color = Color(0xFF111111)
                    )

                    // 2. RECHTER BEREICH: EXPLORER
                    Box(modifier = Modifier.width(300.dp).fillMaxHeight().background(Color(0xFF2B2D30))) {
                        AssetBrowser(projectPath = project.path)
                    }
                }

                // --- DIALOGE ---
                if (showNewProjectDialog) {
                    NewProjectDialog(onDismiss = { showNewProjectDialog = false }, onCreate = { name, width, height ->
                        showNewProjectDialog = false
                        val parentPath = openFolderDialog(window)
                        if (parentPath != null) {
                            val tempConfig = ProjectConfig(name, width, height, path = parentPath)
                            if (createProjectConfig(tempConfig)) {
                                val fullPath = File(parentPath, name).absolutePath
                                onProjectChange(tempConfig.copy(path = fullPath))
                            } else {
                                showLoadError = true
                            }
                        }
                    })
                }

                if (showLoadError) {
                    ErrorDialog(
                        message = "Konnte Projekt nicht laden oder erstellen.", onDismiss = { showLoadError = false })
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapGridArea(gridWidth: Int, gridHeight: Int) {
    // Wir nutzen BoxWithConstraints, um die Größe des Fensters zu kennen.
    // Das erlaubt uns, die Map beim Start zu zentrieren.
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1F22))) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Pixel-Größe der Map berechnen (für die Zentrierung nötig)
        // Hinweis: LocalDensity.current.density gibt uns den Umrechnungsfaktor von DP zu PX
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val mapWidthPx = (gridWidth * TILE_SIZE_DP) * density
        val mapHeightPx = (gridHeight * TILE_SIZE_DP) * density

        // STATE
        var scale by remember { mutableStateOf(1f) }

        // Initialer Offset: Zentriert die Map im sichtbaren Bereich
        var offset by remember {
            mutableStateOf(
                Offset(
                    x = (constraints.maxWidth - mapWidthPx) / 2f,
                    y = (constraints.maxHeight - mapHeightPx) / 2f
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(androidx.compose.ui.graphics.RectangleShape)
                // GESTURES
                .onPointerEvent(PointerEventType.Scroll) {
                    val change = it.changes.first()
                    val delta = change.scrollDelta.y
                    val zoomFactor = if (delta > 0) 0.9f else 1.1f
                    scale = (scale * zoomFactor).coerceIn(0.1f, 10f)
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offset += dragAmount / scale
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x * scale
                        translationY = offset.y * scale
                    }
            ) {
                val step = TILE_SIZE_DP.dp.toPx()

                // Wir nutzen hier direkt die vorberechneten Pixel-Werte
                // (mapWidthPx ist float, wir brauchen es hier aber im DrawScope)

                // 1. Hintergrund der Map
                drawRect(
                    color = Color(0xFF252526),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(mapWidthPx, mapHeightPx)
                )

                // 2. Vertikale Linien
                for (i in 0..gridWidth) {
                    val x = i * step
                    drawLine(
                        color = Color(0xFF3E3E42),
                        start = Offset(x, 0f),
                        end = Offset(x, mapHeightPx),
                        strokeWidth = 1f / scale
                    )
                }

                // 3. Horizontale Linien
                for (i in 0..gridHeight) {
                    val y = i * step
                    drawLine(
                        color = Color(0xFF3E3E42),
                        start = Offset(0f, y),
                        end = Offset(mapWidthPx, y),
                        strokeWidth = 1f / scale
                    )
                }

                // 4. Blauer Rahmen
                drawRect(
                    color = Color(0xFF589DF6),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f / scale),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(mapWidthPx, mapHeightPx)
                )
            }
        }

        // Info Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${(scale * 100).toInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@Composable
fun AssetBrowser(projectPath: String?) {
    // Liste der Dateien im State halten
    var imageFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // Dateien laden, wenn sich der Pfad ändert
    LaunchedEffect(projectPath) {
        if (projectPath != null) {
            val dir = File(projectPath)
            if (dir.exists() && dir.isDirectory) {
                // Filter nach Bildern (png, jpg, etc.)
                imageFiles = dir.listFiles { file ->
                    val ext = file.extension.lowercase()
                    file.isFile && (ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "bmp")
                }?.toList() ?: emptyList()
            }
        } else {
            imageFiles = emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF3C3F41)).padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Explorer", style = MaterialTheme.typography.subtitle2, color = Color.LightGray)
        }

        Divider(color = Color.Black)

        if (imageFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No images found", color = Color.Gray)
            }
        } else {
            // Grid Ansicht der Dateien
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imageFiles) { file ->
                    AssetItem(file)
                }
            }
        }
    }
}

@Composable
fun AssetItem(file: File) {
    // Bitmap laden (einfache Implementierung)
    val bitmap = rememberBitmapFromFile(file)

    Card(
        backgroundColor = Color(0xFF3C3F41),
        elevation = 2.dp,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(100.dp).clickable {
            // TODO: Click Logic (z.B. Tile auswählen)
            println("Selected: ${file.name}")
        }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Bild
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(4.dp), contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap, contentDescription = file.name, contentScale = ContentScale.Fit
                    )
                } else {
                    Text("?", color = Color.Gray)
                }
            }
            // Dateiname
            Text(
                text = file.name,
                style = MaterialTheme.typography.caption,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun rememberBitmapFromFile(file: File): ImageBitmap? {
    var bitmap by remember(file) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(file) {
        try {
            // Versuche Bild zu laden (IO Thread wäre sauberer, für hier reicht es so)
            val bufferedImage = ImageIO.read(file)
            if (bufferedImage != null) {
                bitmap = bufferedImage.toComposeImageBitmap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return bitmap
}
