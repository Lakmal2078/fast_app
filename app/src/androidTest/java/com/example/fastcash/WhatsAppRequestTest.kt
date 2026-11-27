package com.example.fastcash

import android.content.Intent
import android.net.Uri
import com.example.fastcash.ui.components.WhatsAppRequest
import org.junit.Assert.*
import org.junit.Test

class WhatsAppRequestTest {
    @Test fun composeAgentRequestWithOrWithoutScreenshot() {
        val image = Uri.parse("content://media/picker/receipt")
        for ((kind, method) in listOf("Deposit" to "iPay wallet", "Withdrawal" to "Bank payout · Test Bank")) {
            val request = WhatsAppRequest(kind, "TEST0001", 1350, method, image)
            val share = request.intent("TXN-example", "com.whatsapp", "image/png")
            assertEquals(Intent.ACTION_SEND, share.action)
            assertEquals("com.whatsapp", share.`package`)
            assertEquals("94765865387@s.whatsapp.net", share.getStringExtra("jid"))
            assertEquals(image, share.clipData!!.getItemAt(0).uri)
            assertEquals(image, share.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
            assertEquals("image/png", share.type)
            assertTrue(share.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            val caption = share.getStringExtra(Intent.EXTRA_TEXT)!!
            for (field in listOf(kind, method, "TEST0001", "1350", "TXN-example")) assertTrue(field in caption)
            val link = request.copy(screenshot = null).intent("TXN-example", "com.whatsapp.w4b", null)
            assertEquals(Intent.ACTION_VIEW, link.action)
            assertEquals("wa.me", link.data!!.host)
            assertEquals("/94765865387", link.data!!.path)
            assertEquals(caption, link.data!!.getQueryParameter("text"))
        }
    }
}
