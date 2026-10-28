package com.ohav.pixelforge.watch

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.android.apksig.ApkSigner
import com.google.android.wearable.watchface.validator.client.DwfValidatorFactory
import java.io.File
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** A pushable watch face: APK on disk + its Watch Face Push validation token. */
class BuiltFace(val apk: File, val token: String)

/**
 * Builds watch faces on the phone: swaps the face XML inside the CI-built template APK,
 * signs it with a device key (Android Keystore) and validates it to get a push token.
 */
object FaceBuilder {
    private const val TEMPLATE = "custom_template"
    private const val KEY_ALIAS = "watchface_signing"

    /** The template exactly as CI built and validated it. */
    fun template(context: Context): BuiltFace {
        val apk = File(context.cacheDir, "$TEMPLATE.apk")
        context.assets.open("$TEMPLATE.apk").use { input -> apk.outputStream().use { input.copyTo(it) } }
        return BuiltFace(apk, context.assets.open("${TEMPLATE}_token.txt").bufferedReader().readText().trim())
    }

    fun build(context: Context, faceXml: String): BuiltFace {
        val template = template(context).apk
        val unsigned = File(context.cacheDir, "face-unsigned.apk")
        val signed = File(context.cacheDir, "face.apk")
        replaceFaceXml(template, unsigned, faceXml.toByteArray())
        sign(unsigned, signed)
        val result = DwfValidatorFactory.create().validate(signed, context.packageName)
        check(result.failures().isEmpty()) { result.failures().joinToString("\n") { it.failureMessage() } }
        return BuiltFace(signed, result.validationToken())
    }

    /** Copies the template, dropping old signatures and replacing the raw resource that holds the face XML. */
    private fun replaceFaceXml(template: File, out: File, xml: ByteArray) {
        ZipFile(template).use { zip ->
            ZipOutputStream(out.outputStream()).use { zos ->
                var replaced = false
                for (entry in zip.entries()) {
                    if (entry.name.startsWith("META-INF/")) continue
                    var bytes = zip.getInputStream(entry).use { it.readBytes() }
                    if (entry.name.startsWith("res/") && String(bytes.copyOf(minOf(bytes.size, 300))).contains("<WatchFace")) {
                        bytes = xml; replaced = true
                    }
                    val e = ZipEntry(entry.name).apply { method = entry.method }
                    if (entry.method == ZipEntry.STORED) {
                        e.size = bytes.size.toLong(); e.compressedSize = e.size
                        e.crc = CRC32().apply { update(bytes) }.value
                    }
                    zos.putNextEntry(e); zos.write(bytes); zos.closeEntry()
                }
                check(replaced) { "watchface.xml not found in template" }
            }
        }
    }

    /** Signs (v2) with a stable per-device key, so later builds can update the same slot. */
    private fun sign(input: File, output: File) {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(KEY_ALIAS)) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                        .setKeySize(2048)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .build(),
                )
            }.generateKeyPair()
        }
        val key = ks.getKey(KEY_ALIAS, null) as PrivateKey
        val cert = ks.getCertificate(KEY_ALIAS) as X509Certificate
        ApkSigner.Builder(listOf(ApkSigner.SignerConfig.Builder("pixelforge", key, listOf(cert)).build()))
            .setInputApk(input)
            .setOutputApk(output)
            .setMinSdkVersion(33)
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(false)
            .build()
            .sign()
    }
}
