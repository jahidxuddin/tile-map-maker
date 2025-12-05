package de.ju.tilemapmaker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class EditorLayer(
    initialName: String,
    initialVisible: Boolean,
    initialTiles: Map<Pair<Int, Int>, File>
) {
    var name by mutableStateOf(initialName)
    var isVisible by mutableStateOf(initialVisible)
    val tiles = mutableStateMapOf<Pair<Int, Int>, File>().apply { putAll(initialTiles) }
}