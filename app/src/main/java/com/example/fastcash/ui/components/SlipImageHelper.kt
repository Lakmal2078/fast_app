package com.example.fastcash.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

data class ProcessedSlip(
    val base64: String,
    val jpegBytes: ByteArray,
    val shareUri: Uri,
    val sizeKb: Int
)

object SlipImageHelper {
    private const val MAX_DIMENSION = 960
    private const val MAX_JPEG_BYTES = 210_000 // ~280,000 Base64 chars, safely within 350,000 Firestore rules limit

    suspend fun processPickedSlip(context: Context, sourceUri: Uri): Result<ProcessedSlip> =
        withContext(Dispatchers.IO) {
            runCatching {
                val appContext = context.applicationContext
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                appContext.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                } ?: throw IllegalArgumentException("Unable to read selected image")

                val rawWidth = boundsOpts.outWidth
                val rawHeight = boundsOpts.outHeight
                if (rawWidth <= 0 || rawHeight <= 0) {
                    throw IllegalArgumentException("Invalid image file selected")
                }

                var sampleSize = 1
                while (max(rawWidth / sampleSize, rawHeight / sampleSize) > MAX_DIMENSION * 2) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = appContext.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: throw IllegalArgumentException("Unable to decode slip image")

                val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION)
                if (scaled !== decoded) {
                    decoded.recycle()
                }

                var quality = 78
                var bytes: ByteArray
                do {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    bytes = out.toByteArray()
                    quality -= 12
                } while (bytes.size > MAX_JPEG_BYTES && quality >= 30)

                scaled.recycle()

                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                if (base64.length > 340_000) {
                    throw IllegalArgumentException("Slip image is too large. Please pick a clearer cropped screenshot.")
                }

                val slipsDir = File(appContext.cacheDir, "slips").apply { mkdirs() }
                val file = File(slipsDir, "deposit_slip_${System.currentTimeMillis()}.jpg")
                file.writeBytes(bytes)
                val shareUri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file
                )

                ProcessedSlip(
                    base64 = base64,
                    jpegBytes = bytes,
                    shareUri = shareUri,
                    sizeKb = max(1, bytes.size / 1024)
                )
            }
        }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val largest = max(w, h)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val newW = max(1, (w * ratio).toInt())
        val newH = max(1, (h * ratio).toInt())
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
