import com.ohav.pixelforge.editor.*
import java.io.File
fun main() {
    File("out").mkdirs()
    val all = LayerKind.entries.mapIndexed { i, k -> Layer.create(k, i + 1L, 0x80FF3D8B).copy(color2 = 0xFF00C2FF, rotation = 30f, extra = true, weight = i % 5) }
    val styled = FaceDoc(0xFF000000, listOf(
        Layer.create(LayerKind.IMAGE, 1, 0).copy(size = 450f, height = 450f, thickness = 225f),
        Layer.create(LayerKind.GAUGE, 2, 0xFFFF3D8B).copy(extra = true, x = 150f, size = 40f, thickness = 3f, text = "SECONDS"),
        Layer.create(LayerKind.GAUGE, 4, 0xFF2EE59D).copy(color2 = 0xFF00C2FF),
        Layer.create(LayerKind.HANDS, 3, 0xFFFFFFFF).copy(text = "dauphine", extra = true),
    ))
    val faces = listOf("Styled" to styled) + Presets.groups.flatMap { (g, list) -> list.map { (n, d) -> "$g-$n" to d } } + ("All" to FaceDoc(0xFF000000, all, background2 = 0xFF3D7BFF))
    faces.forEach { (n, d) ->
        File("out/${n.replace(' ', '_')}.xml").writeText(FaceXml.build(d))
        check(FaceDoc.fromJson(d.toJson()) == d) { "json roundtrip $n" }
    }
    WidgetPresets.groups.flatMap { it.second }.forEach { (n, d) -> check(FaceDoc.fromJson(d.toJson()) == d) { "widget json $n" } }
    println("ok ${faces.size} faces, ${WidgetPresets.groups.sumOf { it.second.size }} widgets")
}
