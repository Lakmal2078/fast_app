package com.example.fastcash.data.repository

import com.example.fastcash.data.local.AppDatabase
import com.example.fastcash.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class FastCashRepository(private val db: AppDatabase) {
    private val transactionDao = db.transactionDao()
    private val notificationDao = db.notificationDao()
    private val settingsDao = db.settingsDao()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val userProfile: Flow<UserProfileEntity?> = settingsDao.getUserProfile()
    val appSettings: Flow<AppSettingsEntity?> = settingsDao.getAppSettings()
    val activeAgentAccounts: Flow<List<AgentAccountEntity>> = settingsDao.getActiveAgentAccounts()
    val allReconRows: Flow<List<ReconRowEntity>> = settingsDao.getAllReconRows()

    suspend fun seedInitialDataIfEmpty() {
        val existing = allTransactions.firstOrNull()
        if (existing.isNullOrEmpty()) {
            // Seed initial demo transactions
            transactionDao.insert(
                TransactionEntity(
                    reference = "TXN-7F42A",
                    type = TransactionType.DEPOSIT,
                    status = TransactionStatus.PROCESSING,
                    amount = 25000L,
                    playerId = "184920412",
                    paymentMethod = "Bank transfer",
                    bank = "Bank of Ceylon (BOC)",
                    accountNumber = "8823104921",
                    createdAt = System.currentTimeMillis() - 3600000L
                )
            )
            transactionDao.insert(
                TransactionEntity(
                    reference = "TXN-19AC2",
                    type = TransactionType.WITHDRAWAL,
                    status = TransactionStatus.COMPLETED,
                    amount = 12500L,
                    playerId = "184920412",
                    paymentMethod = "Bank payout",
                    securityCode = "8914-XK",
                    bank = "Commercial Bank",
                    accountNumber = "1094002914",
                    createdAt = System.currentTimeMillis() - 86400000L
                )
            )

            // Seed notifications
            notificationDao.insert(
                NotificationEntity(
                    title = "System Ready",
                    message = "Fast Cash official agent bridge is active. Agents online.",
                    isRead = false
                )
            )
            notificationDao.insert(
                NotificationEntity(
                    title = "Request Processing",
                    message = "TXN-7F42A is currently under agent verification.",
                    isRead = false
                )
            )

            // Seed agent accounts
            settingsDao.insertAgentAccount(
                AgentAccountEntity(
                    label = "Bank of Ceylon (BOC) - Fast Cash LK",
                    accountNumber = "8340192841",
                    method = "Bank transfer",
                    isActive = true
                )
            )
            settingsDao.insertAgentAccount(
                AgentAccountEntity(
                    label = "People's Bank - Walasmulla Operations",
                    accountNumber = "2049102941",
                    method = "Bank transfer",
                    isActive = true
                )
            )
            settingsDao.insertAgentAccount(
                AgentAccountEntity(
                    label = "Sampath Bank - Fast Cash Corporate",
                    accountNumber = "1004819204",
                    method = "iPay / Bank",
                    isActive = true
                )
            )

            // Seed settings
            settingsDao.saveAppSettings(AppSettingsEntity())

            // Seed profile
            settingsDao.saveUserProfile(UserProfileEntity(name = "Sujith Lakmal", playerId = "184920412"))

            // Seed recon rows
            settingsDao.insertReconRow(
                ReconRowEntity(
                    externalId = "BOC-99201",
                    provider = StatementProvider.BOC,
                    direction = "CREDIT",
                    amount = 25000L,
                    reference = "TXN-7F42A",
                    description = "BOC FT TXN-7F42A FastCash Deposit",
                    verificationStatus = VerificationStatus.VERIFIED
                )
            )
            settingsDao.insertReconRow(
                ReconRowEntity(
                    externalId = "PB-41092",
                    provider = StatementProvider.PEOPLES_BANK,
                    direction = "CREDIT",
                    amount = 50000L,
                    reference = "Unknown Transfer",
                    description = "Counter deposit cash CDM",
                    verificationStatus = VerificationStatus.UNMATCHED
                )
            )
        }
    }

    private fun generateReference(): String {
        val randomSuffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        return "TXN-$randomSuffix"
    }

    suspend fun createDeposit(
        amount: Long,
        playerId: String,
        paymentMethod: String,
        receiptKey: String?
    ): String {
        val reference = generateReference()
        val transaction = TransactionEntity(
            reference = reference,
            type = TransactionType.DEPOSIT,
            status = TransactionStatus.PENDING,
            amount = amount,
            playerId = playerId,
            paymentMethod = paymentMethod,
            receiptKey = receiptKey
        )
        transactionDao.insert(transaction)
        notificationDao.insert(
            NotificationEntity(
                title = "Deposit Received",
                message = "Request $reference for LKR $amount submitted and under review."
            )
        )
        return reference
    }

    suspend fun createWithdrawal(
        amount: Long,
        playerId: String,
        securityCode: String,
        bank: String,
        accountNumber: String,
        contactNumber: String
    ): String {
        val reference = generateReference()
        val transaction = TransactionEntity(
            reference = reference,
            type = TransactionType.WITHDRAWAL,
            status = TransactionStatus.PENDING,
            amount = amount,
            playerId = playerId,
            paymentMethod = "Bank payout",
            securityCode = securityCode,
            bank = bank,
            accountNumber = accountNumber,
            contactNumber = contactNumber
        )
        transactionDao.insert(transaction)
        notificationDao.insert(
            NotificationEntity(
                title = "Withdrawal Received",
                message = "Request $reference for LKR $amount submitted and queued for payout."
            )
        )
        return reference
    }

    suspend fun findTransactionByReference(ref: String): TransactionEntity? {
        return transactionDao.getByReference(ref.trim())
    }

    suspend fun updateTransactionStatus(id: Long, status: TransactionStatus, reason: String? = null) {
        val tx = transactionDao.getById(id)
        transactionDao.updateStatus(id, status, reason)
        if (tx != null) {
            val title = when (status) {
                TransactionStatus.COMPLETED -> "Request Completed"
                TransactionStatus.REJECTED -> "Request Rejected"
                TransactionStatus.PROCESSING -> "Request Processing"
                TransactionStatus.PENDING -> "Status Updated"
            }
            val msg = if (status == TransactionStatus.REJECTED && !reason.isNullOrBlank()) {
                "${tx.reference} rejected: $reason"
            } else {
                "${tx.reference} status is now $status."
            }
            notificationDao.insert(NotificationEntity(title = title, message = msg))
        }
    }

    suspend fun markNotificationAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun saveUserProfile(name: String, playerId: String) {
        settingsDao.saveUserProfile(UserProfileEntity(id = 1, name = name, playerId = playerId))
    }

    suspend fun saveAppSettings(settings: AppSettingsEntity) {
        settingsDao.saveAppSettings(settings)
    }

    suspend fun addReconRow(row: ReconRowEntity): Long {
        return settingsDao.insertReconRow(row)
    }

    suspend fun updateReconRow(row: ReconRowEntity) {
        settingsDao.updateReconRow(row)
    }
}
