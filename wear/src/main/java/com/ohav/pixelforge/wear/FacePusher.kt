package com.ohav.pixelforge.wear

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.wear.watchfacepush.WatchFacePushManager
import androidx.wear.watchfacepush.WatchFacePushManagerFactory
import java.io.File

/** Installs or replaces a watch face APK through Watch Face Push and tries to make it active. */
object FacePusher {
    suspend fun install(context: Context, apk: File, token: String): String {
        val manager = WatchFacePushManagerFactory.createWatchFacePushManager(context)
        val pkg = context.packageManager.getPackageArchiveInfo(apk.path, 0)?.packageName ?: return "קובץ פני שעון לא תקין"
        val existing = manager.listWatchFaces().installedWatchFaceDetails.firstOrNull { it.packageName == pkg }
        val slot = if (existing == null) {
            open(apk) { manager.addWatchFace(it, token) }.slotId
        } else try {
            open(apk) { manager.updateWatchFace(existing.slotId, it, token) }.slotId
        } catch (e: WatchFacePushManager.UpdateWatchFaceException) {
            // Different signing key or version: replace the slot instead.
            manager.removeWatchFace(existing.slotId)
            open(apk) { manager.addWatchFace(it, token) }.slotId
        }
        return try {
            manager.setWatchFaceAsActive(slot)
            "הותקן והופעל ✓"
        } catch (e: Exception) {
            "הותקן ✓ (להפעלה: לחיצה ארוכה על השעון ובחירה)"
        }
    }

    suspend fun list(context: Context): List<String> =
        WatchFacePushManagerFactory.createWatchFacePushManager(context).listWatchFaces().installedWatchFaceDetails.map { it.packageName.substringAfterLast('.') }

    private inline fun <T> open(apk: File, block: (ParcelFileDescriptor) -> T): T =
        ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY).use(block)
}
