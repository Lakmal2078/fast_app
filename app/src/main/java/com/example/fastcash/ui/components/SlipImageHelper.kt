package com.example.fastcash.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
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
    private const val MAX_RAW_UPLOAD_BYTES = 25 * 1024 * 1024

    suspend fun processPickedSlip(context: Context, sourceUri: Uri): Result<ProcessedSlip> =
        withContext(Dispatchers.IO) {
            runCatching {
                val appContext = context.applicationContext
                val rawBytes = readUriBytes(appContext, sourceUri)

                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                // decodeByteArray intentionally returns null when inJustDecodeBounds = true; inspect outWidth/outHeight.
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)

                val rawWidth = boundsOpts.outWidth
                val rawHeight = boundsOpts.outHeight
                if (rawWidth <= 0 || rawHeight <= 0) {
                    throw IllegalArgumentException("Unsupported or corrupted image file selected")
                }

                var sampleSize = 1
                while (max(rawWidth / sampleSize, rawHeight / sampleSize) > MAX_DIMENSION * 2) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                    ?: throw IllegalArgumentException("Unable to decode slip image")

                val oriented = applyExifOrientation(decoded, rawBytes)
                if (oriented !== decoded) {
                    decoded.recycle()
                }

                val scaled = scaleDownIfNeeded(oriented, MAX_DIMENSION)
                if (scaled !== oriented) {
                    oriented.recycle()
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
                // Keep cache clean from old temporary slips
                slipsDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
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

    private fun readUriBytes(context: Context, uri: Uri): ByteArray {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Unable to open selected image")
        return stream.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(chunk)
                if (read == -1) break
                total += read
                if (total > MAX_RAW_UPLOAD_BYTES) {
                    throw IllegalArgumentException("Image file is too large (max 25 MB)")
                }
                buffer.write(chunk, 0, read)
            }
            val result = buffer.toByteArray()
            if (result.isEmpty()) {
                throw IllegalArgumentException("Selected image file is empty")
            }
            result
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, rawBytes: ByteArray): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(rawBytes)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
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
