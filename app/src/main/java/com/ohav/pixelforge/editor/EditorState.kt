package com.ohav.pixelforge.editor

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Editor document + undo/redo. [update] changes the doc live (drags, sliders); [commit] records
 * the change as one undo step and autosaves. Discrete actions use [edit], which does both.
 */
class EditorState(context: Context) {
    private val file = File(context.filesDir, "faces/current.json")

    var doc by mutableStateOf(load())
        private set
    var selectedId by mutableStateOf(doc.layers.lastOrNull()?.id)
    private var committed = doc
    private val undo = mutableStateListOf<FaceDoc>()
    private val redo = mutableStateListOf<FaceDoc>()

    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()
    val selected get() = doc.layers.firstOrNull { it.id == selectedId }

    fun update(f: (FaceDoc) -> FaceDoc) { doc = f(doc) }

    fun updateLayer(id: Long, f: (Layer) -> Layer) = update { d -> d.copy(layers = d.layers.map { if (it.id == id) f(it) else it }) }

    fun commit() {
        if (doc == committed) return
        undo += committed
        if (undo.size > 100) undo.removeAt(0)
        redo.clear()
        committed = doc
        save()
    }

    fun edit(f: (FaceDoc) -> FaceDoc) { update(f); commit() }

    fun editLayer(id: Long, f: (Layer) -> Layer) { updateLayer(id, f); commit() }

    fun undo() = step(undo, redo)
    fun redo() = step(redo, undo)

    private fun step(from: MutableList<FaceDoc>, to: MutableList<FaceDoc>) {
        val prev = from.removeLastOrNull() ?: return
        to += committed
        doc = prev; committed = prev
        if (selected == null) selectedId = doc.layers.lastOrNull()?.id
        save()
    }

    fun add(kind: LayerKind, accent: Long) = add(Layer.create(kind, doc.nextId, accent))

    /** Adds [layer] on top (its id is replaced with a fresh one) and selects it. */
    fun add(layer: Layer) {
        val l = layer.copy(id = doc.nextId)
        edit { it.copy(layers = it.layers + l) }
        selectedId = l.id
    }

    fun delete(id: Long) {
        edit { d -> d.copy(layers = d.layers.filterNot { it.id == id }) }
        if (selectedId == id) selectedId = doc.layers.lastOrNull()?.id
    }

    /** Moves a layer [delta] steps up (+1 = towards the top of the stack). */
    fun move(id: Long, delta: Int) = edit { d ->
        val list = d.layers.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = (i + delta).coerceIn(0, list.lastIndex)
        if (i < 0 || i == j) d else d.copy(layers = list.apply { add(j, removeAt(i)) })
    }

    /** Topmost visible, unlocked layer under the point (face pixels). */
    fun layerAt(x: Float, y: Float): Layer? = doc.layers.lastOrNull { it.visible && !it.locked && it.hit(x, y) }

    /** Replaces the face with a preset (one undo step). */
    fun apply(preset: FaceDoc) { edit { preset }; selectedId = doc.layers.lastOrNull()?.id }

    private fun load(): FaceDoc = runCatching { FaceDoc.fromJson(file.readText()) }.getOrDefault(FaceDoc.Starter)

    private fun save() {
        file.parentFile?.mkdirs()
        file.writeText(doc.toJson())
    }
}
