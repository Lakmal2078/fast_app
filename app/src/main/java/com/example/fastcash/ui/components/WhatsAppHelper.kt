package com.example.fastcash.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.fastcash.data.model.AgentDestinationAccount
import java.text.NumberFormat
import java.util.Locale

object WhatsAppHelper {

    fun buildDepositMessage(
        reference: String,
        amount: Long,
        playerId: String,
        destination: AgentDestinationAccount?,
        paymentMethod: String,
        receiptRef: String?,
        customerName: String?,
        customerEmail: String?,
        hasSlipImage: Boolean = false,
        ocr: SlipOcrResult? = null
    ): String {
        val formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(amount)
        val lines = mutableListOf(
            "*FAST CASH SRI LANKA - DEPOSIT REQUEST*",
            "Ref: $reference",
            "Amount: LKR $formattedAmount",
            "1xBet Player ID: $playerId",
            "Method: $paymentMethod"
        )
        if (destination != null) {
            lines.add("Deposited To: ${destination.displayLabel} (${destination.accountNumber})")
        }
        if (!receiptRef.isNullOrBlank()) {
            lines.add("Receipt / Slip Ref: $receiptRef")
        }
        lines.add("FastCash Request Status: PENDING (agent verification required)")
        if (ocr != null) {
            lines.add("--- OCR SLIP DETAILS (UNVERIFIED) ---")
            ocr.detectedMethod?.let { lines.add("Slip Method: $it") }
            ocr.detectedAmount?.let { lines.add("Slip Amount: LKR ${NumberFormat.getNumberInstance(Locale.US).format(it)}") }
            ocr.paymentStatus?.let { lines.add("Slip Status (not bank-confirmed): $it") }
            ocr.transactionDate?.let { lines.add("Transaction Date: $it") }
            ocr.detectedReference?.let { lines.add("Transaction ID: $it") }
            ocr.transferType?.let { lines.add("Transfer Type: $it") }
            ocr.sender?.let { lines.add("From: $it") }
            ocr.senderBank?.let { lines.add("Sender Bank: $it") }
            ocr.senderAccount?.let { lines.add("Sender Account: $it") }
            ocr.recipientBank?.let { lines.add("To Bank: $it") }
            ocr.recipientAccount?.let { lines.add("To Account: $it") }
            ocr.recipientName?.let { lines.add("Recipient: $it") }
            ocr.remarks?.let { lines.add("Remarks: $it") }
        }
        if (hasSlipImage) {
            lines.add("Bank Slip Photo: Attached to WhatsApp share")
        }
        val customerLabel = listOfNotNull(
            customerName?.takeIf { it.isNotBlank() },
            customerEmail?.takeIf { it.isNotBlank() }
        ).joinToString(" / ")
        if (customerLabel.isNotBlank()) {
            lines.add("Customer: $customerLabel")
        }
        lines.add("Please credit via MobCash to 1xBet.")
        return lines.joinToString("\n")
    }

    fun buildWithdrawalMessage(
        reference: String,
        amount: Long,
        playerId: String,
        securityCode: String,
        bank: String,
        accountNumber: String,
        accountName: String?,
        contactNumber: String?,
        customerName: String?,
        customerEmail: String?
    ): String {
        val formattedAmount = NumberFormat.getNumberInstance(Locale.US).format(amount)
        val lines = mutableListOf(
            "*FAST CASH SRI LANKA - WITHDRAWAL REQUEST*",
            "Ref: $reference",
            "Amount: LKR $formattedAmount",
            "1xBet Player ID: $playerId",
            "1xBet Security Code: $securityCode",
            "Recipient Bank: $bank",
            "Account Number: $accountNumber"
        )
        if (!accountName.isNullOrBlank()) {
            lines.add("Account Name: $accountName")
        }
        if (!contactNumber.isNullOrBlank()) {
            lines.add("Contact / WhatsApp: $contactNumber")
        }
        val customerLabel = listOfNotNull(
            customerName?.takeIf { it.isNotBlank() },
            customerEmail?.takeIf { it.isNotBlank() }
        ).joinToString(" / ")
        if (customerLabel.isNotBlank()) {
            lines.add("Customer: $customerLabel")
        }
        lines.add("Please process bank payout after MobCash verification.")
        return lines.joinToString("\n")
    }

    fun openWhatsApp(context: Context, phoneNumber: String, message: String? = null) {
        val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
        val url = if (message.isNullOrBlank()) {
            "https://wa.me/$cleanNumber"
        } else {
            "https://wa.me/$cleanNumber?text=${Uri.encode(message)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "WhatsApp is not installed on this device.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun isWhatsAppInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return listOf("com.whatsapp", "com.whatsapp.w4b").any { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }
    }

    fun shareImageToWhatsApp(
        context: Context,
        imageUri: Uri,
        message: String? = null,
        phoneNumber: String? = null
    ) {
        val cleanNumber = phoneNumber?.replace("+", "")?.replace(" ", "")?.replace("-", "")
            ?.takeIf { it.isNotBlank() }
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in packages) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                if (!message.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                if (cleanNumber != null) {
                    putExtra("jid", "$cleanNumber@s.whatsapp.net")
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage(pkg)
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try next WhatsApp package or fallback chooser
            }
        }
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            if (!message.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, message)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(fallback, "Share Deposit Slip"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No app found to share the slip image.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
