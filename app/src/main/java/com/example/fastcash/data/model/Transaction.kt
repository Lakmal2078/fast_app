package com.example.fastcash.data.model

import androidx.room.ColumnInfo
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

/**
 * Room database entity representing a 1xBet wallet transaction log.
 * Stores deposit and withdrawal records including amount, status, timestamp, and user reference.
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "reference")
    val reference: String,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "status")
    val status: TransactionStatus = TransactionStatus.PENDING,

    @ColumnInfo(name = "amount")
    val amount: Long,

    @ColumnInfo(name = "playerId")
    val playerId: String,

    @ColumnInfo(name = "paymentMethod")
    val paymentMethod: String = "Bank transfer",

    @ColumnInfo(name = "securityCode")
    val securityCode: String? = null,

    @ColumnInfo(name = "bank")
    val bank: String? = null,

    @ColumnInfo(name = "accountNumber")
    val accountNumber: String? = null,

    @ColumnInfo(name = "contactNumber")
    val contactNumber: String? = null,

    @ColumnInfo(name = "receiptKey")
    val receiptKey: String? = null,

    @ColumnInfo(name = "rejectionReason")
    val rejectionReason: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Alias for createdAt to maintain compatibility across existing screens & DAOs */
    val createdAt: Long get() = timestamp

    /** Explicit user reference getter */
    val userReference: String get() = reference
}

typealias TransactionEntity = Transaction
