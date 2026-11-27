package com.example.fastcash.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private const val AGENT = "94765865387"

data class WhatsAppRequest(
    val kind: String,
    val playerId: String,
    val amount: Long,
    val method: String,
    val screenshot: Uri?
) {
    fun caption(reference: String) = listOf(
        "Fast Cash · $kind", "1xBet player ID: $playerId", "Amount: LKR $amount",
        "Payment method: $method", "Reference: $reference"
    ).joinToString("\n")

    fun intent(reference: String, packageName: String, mime: String?): Intent {
        val message = caption(reference)
        val image = screenshot
        if (image == null) {
            val url = Uri.parse("https://wa.me/$AGENT").buildUpon()
                .appendQueryParameter("text", message).build()
            return Intent(Intent.ACTION_VIEW, url).setPackage(packageName)
        }
        return Intent(Intent.ACTION_SEND).setPackage(packageName).apply {
            type = mime ?: "image/*"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_STREAM, image)
            putExtra("jid", "$AGENT@s.whatsapp.net")
            clipData = ClipData.newRawUri("Screenshot", image)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

@Composable
fun ScreenshotPicker(image: Uri?, enabled: Boolean, sinhala: Boolean, onChange: (Uri?) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        if (it != null) onChange(it)
    }
    OutlinedButton(onClick = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("pick_screenshot")) {
        Text(if (image == null) {
            if (sinhala) "WhatsApp සඳහා screenshot එකක් තෝරන්න" else "Choose screenshot for WhatsApp"
        } else if (sinhala) "තෝරාගත් screenshot එක වෙනස් කරන්න" else "Change selected screenshot")
    }
    if (image != null) TextButton(onClick = { onChange(null) }, enabled = enabled) {
        Text(if (sinhala) "ඇමුණුම ඉවත් කරන්න" else "Remove attachment")
    }
}

private fun openWhatsApp(context: Context, request: WhatsAppRequest, reference: String): Boolean {
    val mime = request.screenshot?.let { context.contentResolver.getType(it) }
    for (app in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        try {
            context.startActivity(request.intent(reference, app, mime))
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the other WhatsApp edition installed on this phone.
        }
    }
    return false
}

@Composable
fun WhatsAppHandoff(request: WhatsAppRequest, reference: String, sinhala: Boolean) {
    val context = LocalContext.current
    var error by remember(reference) { mutableStateOf<String?>(null) }
    Spacer(Modifier.height(8.dp))
    Text(if (sinhala) "යවන්න පෙර WhatsApp හි ලබන්නා +94765865387 බව බලන්න."
        else "Check that the recipient is +94765865387 before sending.")
    Button(onClick = {
        error = try {
            if (openWhatsApp(context, request, reference)) null
            else if (sinhala) "WhatsApp හෝ WhatsApp Business ස්ථාපනය කර නැවත උත්සාහ කරන්න."
            else "Install WhatsApp or WhatsApp Business and try again."
        } catch (_: SecurityException) {
            if (sinhala) "Screenshot එකට ප්‍රවේශ විය නොහැක." else "The screenshot is no longer accessible."
        }
    }, modifier = Modifier.fillMaxWidth().testTag("whatsapp_request")) {
        Text(if (sinhala) "WhatsApp වෙත යන්න" else "Open request in WhatsApp")
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
