package com.ohav.pixelforge.watch

import android.content.Context
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

/** Phone ↔ watch over the Wear Data Layer. The watch app answers "/wf/push" with a "/wf/result" message. */
object WatchLink {
    /** Name of the connected watch running Pixel Forge, or null. */
    suspend fun watchName(context: Context): String? =
        Wearable.getCapabilityClient(context).getCapability("pixelforge_watch", CapabilityClient.FILTER_REACHABLE).await()
            .nodes.firstOrNull()?.displayName

    /** Sends the face and waits (up to 2 minutes) for the watch's install result. */
    suspend fun push(context: Context, face: BuiltFace): String {
        val messages = Wearable.getMessageClient(context)
        var listener: MessageClient.OnMessageReceivedListener? = null
        return try { withTimeout(120_000) {
            suspendCancellableCoroutine { cont ->
                listener = MessageClient.OnMessageReceivedListener { event ->
                    if (event.path == "/wf/result" && cont.isActive) cont.resume(String(event.data))
                }.also { messages.addListener(it) }
                val request = PutDataMapRequest.create("/wf/push").apply {
                    dataMap.putAsset("apk", Asset.createFromBytes(face.apk.readBytes()))
                    dataMap.putString("token", face.token)
                    dataMap.putLong("time", System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(context).putDataItem(request).addOnFailureListener { if (cont.isActive) cont.resume("שליחה נכשלה: ${it.message}") }
            }
        } } finally { listener?.let { messages.removeListener(it) } }
    }
}
