package com.example.fastcash.data.remote

import com.google.gson.annotations.SerializedName

data class StatusResponse(
    val status: String,
    val service: String,
    val timestamp: Long,
    val version: String
)

data class DepositRequest(
    val amount: Long,
    val playerId: String,
    val paymentMethod: String = "Bank transfer",
    val receiptKey: String? = null,
    val destinationId: Long? = null
)

data class WithdrawalRequest(
    val amount: Long,
    val playerId: String,
    val securityCode: String,
    val bank: String,
    val accountNumber: String,
    val contactNumber: String? = null
)

data class CustomerSession(val customerId: String, val isAgent: Boolean = false)

data class TransactionDto(
    val id: Long = 0,
    val reference: String,
    val type: String,
    val status: String,
    val amount: Long,
    val playerId: String,
    val paymentMethod: String = "Bank transfer",
    val securityCode: String? = null,
    val bank: String? = null,
    val accountNumber: String? = null,
    val contactNumber: String? = null,
    val receiptKey: String? = null,
    val rejectionReason: String? = null,
    val timestamp: Long
)

data class AdminUpdateStatusRequest(
    val status: String,
    val reason: String? = null
)

data class AgentAccountDto(
    val id: Long,
    val label: String,
    val accountNumber: String,
    val method: String,
    @SerializedName("isActive")
    val isActive: Int = 1
)

data class NotificationDto(
    val id: Long,
    val title: String,
    val message: String,
    val isRead: Int = 0,
    val createdAt: Long
)

data class AppSettingsDto(
    val id: Int = 1,
    val whatsappNumber: String,
    val minTransaction: Long,
    val maxTransaction: Long,
    val promoCode: String
)

data class SlipVerifyRequest(
    val image_base64: String,
    val expected_amount: Long? = null,
    val expected_reference: String? = null
)

data class ExtractedSlipData(
    val bank: String,
    val amount: Long,
    val reference: String,
    val date: String,
    val status: String,
    val confidence: Double,
    val raw_text: String? = null
)

data class SlipVerifyResponse(
    val success: Boolean,
    val data: ExtractedSlipData,
    val timestamp: Long
)


data class AgentBankInput(val bank: String, val holder: String, val number: String)
data class AgentReview(val reference: String, val playerId: String, val amount: Long, val status: String,
    val type: String, val paymentMethod: String, val destination: String?, val receiptKey: String?,
    val bank: String?, val accountNumber: String?, val securityCode: String?)
data class PlayerDecision(val id: Long, val reference: String, val type: String, val amount: Long,
    val status: String, val rejectionReason: String?, val createdAt: Long)
