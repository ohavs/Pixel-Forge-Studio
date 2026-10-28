import com.ohav.pixelforge.editor.*
import java.io.File
fun main() {
    File("out").mkdirs()
    val all = LayerKind.entries.mapIndexed { i, k -> Layer.create(k, i + 1L, 0x80FF3D8B).copy(color2 = 0xFF00C2FF, rotation = 30f, extra = true, weight = i % 5) }
    (Presets.all + ("All" to FaceDoc(0xFF000000, all))).forEach { (n, d) ->
        File("out/$n.xml").writeText(FaceXml.build(d))
        check(FaceDoc.fromJson(d.toJson()) == d) { "json roundtrip $n" }
    }
    println("ok")
}
