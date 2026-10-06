package com.example.fastcash.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "",
    val playerId: String = ""
)

enum class StatementProvider {
    BOC,
    PEOPLES_BANK,
    LOLC,
    SAMPATH,
    IPAY
}

enum class VerificationStatus {
    UNMATCHED,
    AMBIGUOUS,
    VERIFIED,
    EXCEPTION
}

@Entity(tableName = "recon_rows")
data class ReconRowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val externalId: String,
    val provider: StatementProvider,
    val direction: String, // CREDIT or DEBIT
    val amount: Long,
    val reference: String? = null,
    val description: String? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNMATCHED,
    val transactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_accounts")
data class AgentAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val accountNumber: String,
    val method: String,
    val isActive: Boolean = true
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val whatsappNumber: String = "+94765865387",
    val minTransaction: Long = 1000L,
    val maxTransaction: Long = 500000L,
    val promoCode: String = "VGSL"
)
