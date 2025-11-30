package de.ju.tilemapmaker

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import java.io.File
import javax.imageio.ImageIO

const val TILE_SIZE_DP = 32
typealias TileMap = MutableMap<Pair<Int, Int>, File>

@Composable
fun ApplicationScope.EditorWindow(
    project: ProjectConfig, onClose: () -> Unit, onCloseProject: () -> Unit, onProjectChange: (ProjectConfig) -> Unit
) {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

    // Dialog States
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showLoadError by remember { mutableStateOf(false) }

    // Editor States
    var selectedAsset by remember { mutableStateOf<File?>(null) }
    val placedTiles = remember { mutableStateMapOf<Pair<Int, Int>, File>() }

    Window(
        onCloseRequest = onClose,
        state = windowState,
        undecorated = true,
        title = "Tile Map Maker - ${project.name}",
    ) {
        MaterialTheme(colors = darkColors()) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. Title Bar mit Logik
                CustomTitleBar(
                    windowState = windowState,
                    onClose = onClose,
                    windowScope = this@Window,
                    triggerOpenProject = {
                        // Logik für "Open" im Editor
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
                    triggerCloseProject = onCloseProject,
                    showDropdownMenu = true
                )

                // 2. Split View (Editor Area)
                Row(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1F22))) {

                    // Linker Bereich: Grid
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        MapGridArea(
                            gridWidth = project.width,
                            gridHeight = project.height,
                            selectedAsset = selectedAsset,
                            placedTiles = placedTiles,
                            onPlaceTile = { x, y, file ->
                                placedTiles[x to y] = file
                            },
                            onRemoveTile = { x, y ->
                                placedTiles.remove(x to y)
                            })
                    }

                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color(0xFF111111))

                    // Rechter Bereich: Explorer
                    Box(modifier = Modifier.width(300.dp).fillMaxHeight().background(Color(0xFF2B2D30))) {
                        AssetBrowser(
                            projectPath = project.path,
                            selectedAsset = selectedAsset,
                            onAssetSelect = { selectedAsset = it })
                    }
                }

                // --- 3. DIALOGE (WIEDER EINGEFÜGT) ---

                // Dialog für Neues Projekt
                if (showNewProjectDialog) {
                    NewProjectDialog(onDismiss = { showNewProjectDialog = false }, onCreate = { name, width, height ->
                        showNewProjectDialog = false

                        // Speicherort wählen
                        val parentPath = openFolderDialog(window)
                        if (parentPath != null) {
                            val tempConfig = ProjectConfig(name, width, height, path = parentPath)

                            // Erstellen und Wechseln
                            if (createProjectConfig(tempConfig)) {
                                val fullPath = File(parentPath, name).absolutePath
                                onProjectChange(tempConfig.copy(path = fullPath))
                            } else {
                                showLoadError = true
                            }
                        }
                    })
                }

                // Fehler Dialog
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
fun MapGridArea(
    gridWidth: Int,
    gridHeight: Int,
    selectedAsset: File?,
    placedTiles: TileMap,
    onPlaceTile: (Int, Int, File) -> Unit,
    onRemoveTile: (Int, Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1F22))) {
        val bitmapCache = remember { mutableMapOf<File, ImageBitmap>() }
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val tileSizePx = TILE_SIZE_DP * density
        val mapWidthPx = (gridWidth * TILE_SIZE_DP) * density
        val mapHeightPx = (gridHeight * TILE_SIZE_DP) * density

        var scale by remember { mutableStateOf(1f) }
        var offset by remember {
            mutableStateOf(
                Offset(
                    (constraints.maxWidth - mapWidthPx) / 2f, (constraints.maxHeight - mapHeightPx) / 2f
                )
            )
        }
        var isLocked by remember { mutableStateOf(false) }

        // Hilfsfunktion: Screen -> Grid
        fun getGridPos(screenPos: Offset): Pair<Int, Int>? {
            val mapX = (screenPos.x / scale) - offset.x
            val mapY = (screenPos.y / scale) - offset.y
            val gridX = (mapX / tileSizePx).toInt()
            val gridY = (mapY / tileSizePx).toInt()
            return if (gridX in 0 until gridWidth && gridY in 0 until gridHeight) gridX to gridY else null
        }

        Box(
            modifier = Modifier.fillMaxSize().clip(RectangleShape)
            // 1. ZOOM (Scrollen)
            .onPointerEvent(PointerEventType.Scroll) {
                val change = it.changes.first()
                scale = (scale * (if (change.scrollDelta.y > 0) 0.9f else 1.1f)).coerceIn(0.1f, 10f)
            }
            // 2. DRAG & MOVE LOGIK (Der Ersatz für detectDragGestures)
            .onPointerEvent(PointerEventType.Move) { event ->
                val change = event.changes.first()
                val dragAmount = change.positionChange()

                // Nur reagieren, wenn die Maus gedrückt ist UND sich bewegt
                if (change.pressed && dragAmount != Offset.Zero) {
                    val isLeftClick = event.buttons.isPrimaryPressed
                    val isRightClick = event.buttons.isSecondaryPressed

                    if (!isLocked) {
                        // Pan Modus: Nur Linksklick verschiebt die Karte
                        if (isLeftClick) {
                            offset += dragAmount / scale
                            change.consume()
                        }
                    } else {
                        // Lock Modus: Malen oder Radieren
                        val gridPos = getGridPos(change.position)
                        if (gridPos != null) {
                            if (isRightClick) {
                                // Rechts ziehen -> Radieren
                                onRemoveTile(gridPos.first, gridPos.second)
                                change.consume()
                            } else if (isLeftClick && selectedAsset != null) {
                                // Links ziehen -> Malen
                                onPlaceTile(gridPos.first, gridPos.second, selectedAsset)
                                change.consume()
                            }
                        }
                    }
                }
            }
            // 3. EINZEL-KLICK (Rechts -> Löschen)
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.buttons.isSecondaryPressed) {
                    getGridPos(event.changes.first().position)?.let { onRemoveTile(it.first, it.second) }
                }
            }
            // 4. EINZEL-TAP (Links -> Malen)
            // Hier nutzen wir weiter pointerInput, da es sauberer Klicks erkennt als onPointerEvent(Press) für Links
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    // Nur reagieren, wenn wir auch ein Asset haben (oder gelockt sind)
                    if (selectedAsset != null) {
                        getGridPos(tapOffset)?.let { onPlaceTile(it.first, it.second, selectedAsset) }
                    }
                }
            }) {
            Canvas(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x * scale
                    translationY = offset.y * scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }) {
                // Hintergrund
                drawRect(
                    Color(0xFF252526),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(mapWidthPx, mapHeightPx)
                )

                // Tiles Zeichnen
                placedTiles.forEach { (pos, file) ->
                    val (x, y) = pos
                    var bmp = bitmapCache[file]
                    if (bmp == null) {
                        try {
                            bmp = ImageIO.read(file)?.toComposeImageBitmap()?.also { bitmapCache[file] = it }
                        } catch (_: Exception) {
                        }
                    }
                    if (bmp != null) {
                        drawImage(
                            bmp, dstOffset = androidx.compose.ui.unit.IntOffset(
                                (x * tileSizePx).toInt(), (y * tileSizePx).toInt()
                            ), dstSize = androidx.compose.ui.unit.IntSize(tileSizePx.toInt(), tileSizePx.toInt())
                        )
                    }
                }

                // Gitter Linien
                val step = tileSizePx
                for (i in 0..gridWidth) drawLine(
                    Color(0xFF3E3E42),
                    start = Offset(i * step, 0f),
                    end = Offset(i * step, mapHeightPx),
                    strokeWidth = 1f / scale
                )
                for (i in 0..gridHeight) drawLine(
                    Color(0xFF3E3E42),
                    start = Offset(0f, i * step),
                    end = Offset(mapWidthPx, i * step),
                    strokeWidth = 1f / scale
                )

                // Blauer Rahmen
                drawRect(
                    Color(0xFF589DF6),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f / scale),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(mapWidthPx, mapHeightPx)
                )
            }
        }

        // Info Overlay
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                .background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = { isLocked = !isLocked }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        "Lock",
                        tint = if (isLocked) Color(0xFFE57373) else Color.White.copy(0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val toolText = if (selectedAsset != null) selectedAsset.name else "None"
                Text(
                    "${(scale * 100).toInt()}% | $toolText",
                    color = Color.White,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

@Composable
fun AssetBrowser(projectPath: String?, selectedAsset: File?, onAssetSelect: (File) -> Unit) {
    var imageFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // ... (Dateien laden Logik bleibt gleich) ...
    LaunchedEffect(projectPath) {
        if (projectPath != null) {
            val dir = File(projectPath)
            if (dir.exists() && dir.isDirectory) {
                imageFiles = dir.listFiles { file ->
                    val ext = file.extension.lowercase()
                    file.isFile && (ext == "png" || ext == "jpg" || ext == "jpeg" || ext == "bmp")
                }?.toList() ?: emptyList()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ... (Header bleibt gleich) ...
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF3C3F41)).padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Explorer", style = MaterialTheme.typography.subtitle2, color = Color.LightGray)
        }
        Divider(color = Color.Black)

        if (imageFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No images found", color = Color.Gray
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(80.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imageFiles) { file ->
                    AssetItem(
                        file = file, isSelected = file == selectedAsset, // Prüfen ob ausgewählt
                        onClick = { onAssetSelect(file) })
                }
            }
        }
    }
}

@Composable
fun AssetItem(file: File, isSelected: Boolean, onClick: () -> Unit) {
    val bitmap = rememberBitmapFromFile(file)

    // Visuelles Feedback für Auswahl: Blauer Rahmen oder Standard Hintergrund
    val border = if (isSelected) BorderStroke(2.dp, Color(0xFF589DF6)) else null
    val elevation = if (isSelected) 8.dp else 2.dp

    Card(
        backgroundColor = Color(0xFF3C3F41), elevation = elevation, border = border, // Rahmen hinzufügen
        modifier = Modifier.height(100.dp).clickable(onClick = onClick) // Callback aufrufen
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.weight(1f).padding(4.dp), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap, null, contentScale = ContentScale.Fit) else Text(
                    "?", color = Color.Gray
                )
            }
            Text(
                file.name,
                style = MaterialTheme.typography.caption,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(4.dp)
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
