package com.ohav.pixelforge.wear

import com.google.android.gms.wearable.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.io.File

/** Receives "/wf/push" data items (APK asset + token) from the phone and reports back on "/wf/result". */
class PushListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == "/wf/push" }.forEach { event ->
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            val node = event.dataItem.uri.host ?: return@forEach
            runBlocking {
                val result = try {
                    val asset = map.getAsset("apk") ?: error("חסר קובץ")
                    val apk = File(cacheDir, "push.apk")
                    Wearable.getDataClient(this@PushListenerService).getFdForAsset(asset).await().inputStream.use { input ->
                        apk.outputStream().use { input.copyTo(it) }
                    }
                    FacePusher.install(this@PushListenerService, apk, map.getString("token") ?: "")
                } catch (e: Exception) {
                    "שגיאה: ${e.javaClass.simpleName} ${e.message ?: ""}"
                }
                Wearable.getMessageClient(this@PushListenerService).sendMessage(node, "/wf/result", result.toByteArray()).await()
            }
        }
    }
}
