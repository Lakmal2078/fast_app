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
        customerEmail: String?
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
}
