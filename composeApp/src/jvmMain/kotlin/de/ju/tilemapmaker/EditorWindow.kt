package de.ju.tilemapmaker

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

const val TILE_SIZE_DP = 32

@Composable
fun ApplicationScope.EditorWindow(
    project: ProjectConfig, onClose: () -> Unit, onCloseProject: () -> Unit, onProjectChange: (ProjectConfig) -> Unit
) {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showLoadError by remember { mutableStateOf(false) }
    var selectedAsset by remember { mutableStateOf<File?>(null) }

    // --- INITIALISIERUNG DER LAYER ---
    val layers = remember(project) {
        val list = mutableStateListOf<EditorLayer>()

        // 1. Migration alter Daten (Falls "oldTiles" existieren)
        val migratedTiles = mutableMapOf<Pair<Int, Int>, File>()
        if (!project.oldTiles.isNullOrEmpty() && project.path != null) {
            project.oldTiles.forEach { (key, filename) ->
                try {
                    val parts = key.split(",")
                    if (parts.size == 2) {
                        val f = File(project.path, filename)
                        if (f.exists()) migratedTiles[parts[0].toInt() to parts[1].toInt()] = f
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Layer aus Config laden
        if (project.layers.isEmpty()) {
            // Wenn keine Layer da sind (neues oder ganz altes Projekt), erstelle Default Layer
            // Fügen ggf. migrierte Tiles hinzu
            list.add(EditorLayer("Background", true, migratedTiles))
        } else {
            project.layers.forEachIndexed { idx, layerConfig ->
                val tileMap = mutableMapOf<Pair<Int, Int>, File>()
                if (project.path != null) {
                    layerConfig.tiles.forEach { (key, filename) ->
                        try {
                            val parts = key.split(",")
                            if (parts.size == 2) {
                                val f = File(project.path, filename)
                                if (f.exists()) tileMap[parts[0].toInt() to parts[1].toInt()] = f
                            }
                        } catch (_: Exception) {}
                    }
                }
                // Migration: Falls wir im ersten Layer sind, fügen wir alte Tiles hinzu
                if (idx == 0 && migratedTiles.isNotEmpty()) {
                    tileMap.putAll(migratedTiles)
                }
                list.add(EditorLayer(layerConfig.name, layerConfig.isVisible, tileMap))
            }
        }
        list
    }

    var selectedLayerIndex by remember { mutableStateOf(0) }

    // --- SPEICHERN ---
    fun saveCurrentState() {
        val newLayerConfigs = layers.map { editorLayer ->
            // Konvertiere TileMap (Int/Int -> File) zu StringMap ("x,y" -> "filename")
            val tileMapStr = editorLayer.tiles.entries.associate { (pos, file) ->
                "${pos.first},${pos.second}" to file.name
            }
            LayerConfig(editorLayer.name, editorLayer.isVisible, tileMapStr)
        }
        val updatedConfig = project.copy(layers = newLayerConfigs)
        saveProjectConfig(updatedConfig)
        onProjectChange(updatedConfig)
    }

    // --- EXPORT ---
    fun exportMapToTxt() {
        if (project.path == null) return

        // 1. Globale Legende erstellen
        val allTiles = layers.flatMap { it.tiles.values }.distinctBy { it.name }.sortedBy { it.name }
        val fileToId = allTiles.mapIndexed { index, file -> file to index }.toMap()

        val sbLegend = StringBuilder()
        fileToId.forEach { (file, id) -> sbLegend.append("$id: ${file.name}\n") }
        File(project.path, "map_legend.txt").writeText(sbLegend.toString())

        // 2. Layer einzeln exportieren
        layers.forEachIndexed { index, layer ->
            val sbGrid = StringBuilder()
            for (y in 0 until project.height) {
                val row = mutableListOf<String>()
                for (x in 0 until project.width) {
                    val file = layer.tiles[x to y]
                    val id = if (file != null) fileToId[file] else -1
                    row.add(id.toString())
                }
                sbGrid.append(row.joinToString(",")).append("\n")
            }
            // Dateiname säubern
            val safeName = layer.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            File(project.path, "map_layer_${index}_$safeName.txt").writeText(sbGrid.toString())
        }
        println("Export completed.")
    }

    Window(onCloseRequest = onClose, state = windowState, undecorated = true, title = "Tile Map Maker - ${project.name}") {
        MaterialTheme(colors = darkColors()) {
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTitleBar(
                    windowState, onClose, this@Window,
                    triggerOpenProject = {
                        val path = openFolderDialog(window)
                        if (path != null) {
                            val config = readProjectConfig(path)
                            if (config != null) onProjectChange(config.copy(path = path)) else showLoadError = true
                        }
                    },
                    triggerNewProject = { showNewProjectDialog = true },
                    triggerCloseProject = onCloseProject,
                    triggerExportAsTxt = { exportMapToTxt() },
                    triggerSaveProject = { saveCurrentState() },
                    showDropdownMenu = true
                )

                Row(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1F22))) {
                    // LINKER BEREICH: GRID
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        MapGridArea(
                            gridWidth = project.width,
                            gridHeight = project.height,
                            selectedAsset = selectedAsset,
                            layers = layers, // Übergebe alle Layer zum Rendern
                            selectedLayerIndex = selectedLayerIndex,
                            onPlaceTile = { x, y, file ->
                                // Platzieren nur auf dem ausgewählten Layer, wenn existent
                                if (selectedLayerIndex in layers.indices) {
                                    layers[selectedLayerIndex].tiles[x to y] = file
                                }
                            },
                            onRemoveTile = { x, y ->
                                // Entfernen nur vom ausgewählten Layer
                                if (selectedLayerIndex in layers.indices) {
                                    layers[selectedLayerIndex].tiles.remove(x to y)
                                }
                            }
                        )
                    }
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color(0xFF111111))

                    // RECHTER BEREICH: SIDEBAR
                    Column(modifier = Modifier.width(300.dp).fillMaxHeight().background(Color(0xFF2B2D30))) {
                        // 1. Layer Manager
                        LayerManager(
                            layers = layers,
                            selectedIndex = selectedLayerIndex,
                            onSelect = { selectedLayerIndex = it },
                            onAddLayer = {
                                // Neuen Layer oben einfügen und auswählen
                                layers.add(0, EditorLayer("Layer ${layers.size}", true, emptyMap()))
                                selectedLayerIndex = 0
                            },
                            onRemoveLayer = { index ->
                                if (layers.size > 1) {
                                    layers.removeAt(index)
                                    // Index korrigieren, falls nötig
                                    if (selectedLayerIndex >= layers.size) selectedLayerIndex = layers.size - 1
                                }
                            }
                        )
                        Divider(color = Color.Black, thickness = 2.dp)

                        // 2. Asset Browser
                        Box(modifier = Modifier.weight(1f)) {
                            AssetBrowser(project.path, selectedAsset) { selectedAsset = it }
                        }
                    }
                }

                if (showNewProjectDialog) NewProjectDialog({ showNewProjectDialog = false }, { n, w, h ->
                    showNewProjectDialog = false
                    val path = openFolderDialog(window)
                    if (path != null) {
                        val cfg = ProjectConfig(n, w, h, path)
                        if (createProjectConfig(cfg)) onProjectChange(cfg.copy(path = File(path, n).absolutePath)) else showLoadError = true
                    }
                })
                if (showLoadError) ErrorDialog("Fehler beim Laden.") { showLoadError = false }
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
    layers: List<EditorLayer>, // Liste aller Layer zum Zeichnen
    selectedLayerIndex: Int,
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
        var offset by remember { mutableStateOf(Offset((constraints.maxWidth - mapWidthPx) / 2f, (constraints.maxHeight - mapHeightPx) / 2f)) }
        var isLocked by remember { mutableStateOf(true) }

        fun getGridPos(screenPos: Offset): Pair<Int, Int>? {
            val mapX = (screenPos.x / scale) - offset.x
            val mapY = (screenPos.y / scale) - offset.y
            val gridX = (mapX / tileSizePx).toInt()
            val gridY = (mapY / tileSizePx).toInt()
            return if (gridX in 0 until gridWidth && gridY in 0 until gridHeight) gridX to gridY else null
        }

        Box(modifier = Modifier.fillMaxSize().clip(RectangleShape).onPointerEvent(PointerEventType.Scroll) {
            val change = it.changes.first()
            scale = (scale * (if (change.scrollDelta.y > 0) 0.9f else 1.1f)).coerceIn(0.1f, 10f)
        }.onPointerEvent(PointerEventType.Move) { event ->
            val change = event.changes.first()
            val dragAmount = change.positionChange()
            if (change.pressed && dragAmount != Offset.Zero) {
                val isLeftClick = event.buttons.isPrimaryPressed
                val isRightClick = event.buttons.isSecondaryPressed
                if (!isLocked) {
                    if (isLeftClick) {
                        offset += dragAmount / scale
                        change.consume()
                    }
                } else {
                    val gridPos = getGridPos(change.position)
                    if (gridPos != null) {
                        if (isRightClick) {
                            onRemoveTile(gridPos.first, gridPos.second)
                            change.consume()
                        } else if (isLeftClick && selectedAsset != null) {
                            onPlaceTile(gridPos.first, gridPos.second, selectedAsset)
                            change.consume()
                        }
                    }
                }
            }
        }.onPointerEvent(PointerEventType.Press) { event ->
            if (event.buttons.isSecondaryPressed) {
                getGridPos(event.changes.first().position)?.let { onRemoveTile(it.first, it.second) }
            }
        }.pointerInput(Unit) {
            detectTapGestures { tapOffset ->
                if (selectedAsset != null) getGridPos(tapOffset)?.let { onPlaceTile(it.first, it.second, selectedAsset) }
            }
        }) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale; scaleY = scale; translationX = offset.x * scale; translationY = offset.y * scale; transformOrigin = TransformOrigin(0f, 0f)
            }) {
                drawRect(Color(0xFF252526), topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(mapWidthPx, mapHeightPx))

                // Layers Rendern: Von hinten (Index size-1) nach vorne (Index 0)
                for (i in layers.indices.reversed()) {
                    val layer = layers[i]
                    if (!layer.isVisible) continue

                    // Transparenz für inaktive Layer könnte hier eingebaut werden (alpha in drawImage)
                    layer.tiles.forEach { (pos, file) ->
                        val (x, y) = pos
                        var bmp = bitmapCache[file]
                        if (bmp == null) {
                            try { bmp = ImageIO.read(file)?.toComposeImageBitmap()?.also { bitmapCache[file] = it } } catch(_: Exception){}
                        }
                        if (bmp != null) {
                            drawImage(
                                bmp,
                                dstOffset = androidx.compose.ui.unit.IntOffset((x * tileSizePx).toInt(), (y * tileSizePx).toInt()),
                                dstSize = androidx.compose.ui.unit.IntSize(tileSizePx.toInt(), tileSizePx.toInt())
                            )
                        }
                    }
                }

                for (i in 0..gridWidth) drawLine(Color(0xFF3E3E42), start = Offset(i * tileSizePx, 0f), end = Offset(i * tileSizePx, mapHeightPx), strokeWidth = 1f / scale)
                for (i in 0..gridHeight) drawLine(Color(0xFF3E3E42), start = Offset(0f, i * tileSizePx), end = Offset(mapWidthPx,
                    i * tileSizePx
                ), strokeWidth = 1f / scale)
                drawRect(Color(0xFF589DF6), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f / scale), topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(mapWidthPx, mapHeightPx))
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp))) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                IconButton(onClick = { isLocked = !isLocked }, modifier = Modifier.size(24.dp)) {
                    Icon(if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, "Lock", tint = if (isLocked) Color(0xFFE57373) else Color.White.copy(0.8f), modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                val activeLayerName = if (selectedLayerIndex in layers.indices) layers[selectedLayerIndex].name else "-"
                val toolText = if (selectedAsset != null) selectedAsset.name else "None"
                Text("${(scale * 100).toInt()}% | Layer: $activeLayerName | $toolText", color = Color.White, style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
fun AssetBrowser(projectPath: String?, selectedAsset: File?, onAssetSelect: (File) -> Unit) {
    var imageFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(projectPath) {
        if (projectPath != null) {
            withContext(Dispatchers.IO) {
                val dir = File(projectPath)
                if (dir.exists() && dir.isDirectory) {
                    val extensions = setOf("png", "jpg", "jpeg", "bmp")

                    imageFiles = dir.walk()
                        .filter { file ->
                            file.isFile && (file.extension.lowercase() in extensions)
                        }
                        .toList()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imageFiles) { file ->
                    AssetItem(
                        file = file, isSelected = file == selectedAsset, onClick = { onAssetSelect(file) })
                }
            }
        }
    }
}

@Composable
fun AssetItem(file: File, isSelected: Boolean, onClick: () -> Unit) {
    val bitmap = rememberBitmapFromFile(file)

    val border = if (isSelected) BorderStroke(2.dp, Color(0xFF589DF6)) else null
    val elevation = if (isSelected) 8.dp else 2.dp

    Card(
        backgroundColor = Color(0xFF3C3F41),
        elevation = elevation,
        border = border,
        // ÄNDERUNG 2: Höhe deutlich erhöht (140dp)
        modifier = Modifier.height(140.dp).clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.weight(1f).padding(8.dp), contentAlignment = Alignment.Center) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None
                    )
                } else {
                    Text("?", color = Color.Gray)
                }
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
            val originalImage = ImageIO.read(file)
            if (originalImage != null) {
                val w = originalImage.width
                val h = originalImage.height
                if (w < 64 || h < 64) {
                    val scale = (128 / w).coerceAtLeast(128 / h).coerceAtLeast(1)
                    if (scale > 1) {
                        val newW = w * scale
                        val newH = h * scale
                        val scaledImage = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
                        val g = scaledImage.createGraphics()
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
                        g.drawImage(originalImage, 0, 0, newW, newH, null)
                        g.dispose()
                        bitmap = scaledImage.toComposeImageBitmap()
                    } else {
                        bitmap = originalImage.toComposeImageBitmap()
                    }
                } else {
                    bitmap = originalImage.toComposeImageBitmap()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
    return bitmap
}

@Composable
fun LayerManager(
    layers: MutableList<EditorLayer>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onAddLayer: () -> Unit,
    onRemoveLayer: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().height(250.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF3C3F41)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Layers", style = MaterialTheme.typography.subtitle2, color = Color.LightGray)
            IconButton(onClick = onAddLayer, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Add, "Add", tint = Color.LightGray)
            }
        }
        Divider(color = Color.Black)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // itemsIndexed für Zugriff auf Index
            itemsIndexed(layers) { index, layer ->
                val isSelected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) Color(0xFF4B6EAF) else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sichtbarkeit Toggle
                    IconButton(
                        onClick = { layer.isVisible = !layer.isVisible },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null,
                            tint = if (isSelected) Color.White else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = layer.name,
                        color = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body2
                    )

                    // Löschen Button (nur wenn mehr als 1 Layer existiert)
                    if (layers.size > 1) {
                        IconButton(onClick = { onRemoveLayer(index) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Del", tint = if (isSelected) Color.White.copy(0.7f) else Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Divider(color = Color(0xFF333333), thickness = 0.5.dp)
            }
        }
    }
}