package com.example.fastcash.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL
}

enum class TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    REJECTED
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reference: String,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val amount: Long,
    val playerId: String,
    val paymentMethod: String = "Bank transfer",
    val securityCode: String? = null,
    val bank: String? = null,
    val accountNumber: String? = null,
    val contactNumber: String? = null,
    val receiptKey: String? = null,
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
