package com.example.fastcash.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class AgentDestinationAccount(
    val id: String,
    val bankName: String,
    val branch: String,
    val accountHolder: String,
    val accountNumber: String,
    val method: String
) {
    val displayLabel: String
        get() = if (branch.isNotBlank()) "$bankName ($branch) · $accountHolder" else "$bankName · $accountHolder"
}

object DefaultAgentAccounts {
    val accounts: List<AgentDestinationAccount> = listOf(
        AgentDestinationAccount(
            id = "pb_walasmulla",
            bankName = "People's Bank",
            branch = "Walasmulla Branch",
            accountHolder = "VGS Lakmal",
            accountNumber = "120200380030196",
            method = "Bank transfer"
        ),
        AgentDestinationAccount(
            id = "sampath_neluwa",
            bankName = "Sampath Bank",
            branch = "Neluwa Branch",
            accountHolder = "NKS Oshadhi",
            accountNumber = "105456146706",
            method = "Bank transfer"
        ),
        AgentDestinationAccount(
            id = "boc_walasmulla",
            bankName = "Bank of Ceylon",
            branch = "Walasmulla Branch",
            accountHolder = "Pasindu Madushan",
            accountNumber = "5014292",
            method = "Bank transfer"
        ),
        AgentDestinationAccount(
            id = "ipay_1",
            bankName = "iPay Wallet",
            branch = "Primary Number",
            accountHolder = "Fast Cash Agent",
            accountNumber = "0740452530",
            method = "iPay wallet"
        ),
        AgentDestinationAccount(
            id = "ipay_2",
            bankName = "iPay Wallet",
            branch = "Secondary Number",
            accountHolder = "Fast Cash Agent",
            accountNumber = "0703346455",
            method = "iPay wallet"
        )
    )
}

@Keep
@IgnoreExtraProperties
data class FirestoreTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String? = null,
    val userName: String? = null,
    val reference: String = "",
    val type: String = "DEPOSIT",
    val status: String = "PENDING",
    val amount: Long = 0L,
    val playerId: String = "",
    val paymentMethod: String = "Bank transfer",
    val agentAccountLabel: String? = null,
    val agentAccountNumber: String? = null,
    val receiptKey: String? = null,
    val securityCode: String? = null,
    val bank: String? = null,
    val accountNumber: String? = null,
    val contactNumber: String? = null,
    val rejectionReason: String? = null,
    val agentNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: TransactionType
        get() = enumValues<TransactionType>().firstOrNull { it.name == type } ?: TransactionType.DEPOSIT

    val parsedStatus: TransactionStatus
        get() = enumValues<TransactionStatus>().firstOrNull { it.name == status } ?: TransactionStatus.PENDING

    val timestampMillis: Long
        get() = (createdAt ?: updatedAt ?: Timestamp.now()).toDate().time

    fun toEntity(): TransactionEntity = TransactionEntity(
        id = 0L,
        reference = reference,
        type = parsedType,
        status = parsedStatus,
        amount = amount,
        playerId = playerId,
        paymentMethod = paymentMethod,
        securityCode = securityCode,
        bank = bank,
        accountNumber = accountNumber,
        contactNumber = contactNumber,
        receiptKey = receiptKey,
        rejectionReason = rejectionReason,
        timestamp = timestampMillis
    )
}

@Keep
@IgnoreExtraProperties
data class FirestoreNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "SUBMITTED",
    val transactionRef: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val createdAtMillis: Long
        get() = (createdAt ?: Timestamp.now()).toDate().time
}

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val playerId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreAppConfig(
    @DocumentId val id: String = "",
    val whatsappNumber: String = "+94765865387",
    val minTransaction: Long = 1000L,
    val maxTransaction: Long = 500000L,
    val promoCode: String = "VGSL",
    @ServerTimestamp val updatedAt: Timestamp? = null
)
