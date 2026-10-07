package com.example.fastcash.data.repository

import com.example.fastcash.data.local.AppDatabase
import com.example.fastcash.data.model.*
import com.example.fastcash.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class FastCashRepository(
    private val db: AppDatabase,
    private val api: FastCashApiService = NetworkClient.apiService
) {
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
                    timestamp = System.currentTimeMillis() - 3600000L
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
                    timestamp = System.currentTimeMillis() - 86400000L
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

        // Attempt initial sync from Cloudflare backend
        syncFromRemote()
    }

    /**
     * Syncs remote Cloudflare Worker state into local Room DB (Offline-First cache)
     */
    suspend fun syncFromRemote() = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getTransactions()
            if (response.isSuccessful) {
                response.body()?.forEach { dto ->
                    val entity = TransactionEntity(
                        id = dto.id,
                        reference = dto.reference,
                        type = runCatching { TransactionType.valueOf(dto.type) }.getOrDefault(TransactionType.DEPOSIT),
                        status = runCatching { TransactionStatus.valueOf(dto.status) }.getOrDefault(TransactionStatus.PENDING),
                        amount = dto.amount,
                        playerId = dto.playerId,
                        paymentMethod = dto.paymentMethod,
                        securityCode = dto.securityCode,
                        bank = dto.bank,
                        accountNumber = dto.accountNumber,
                        contactNumber = dto.contactNumber,
                        receiptKey = dto.receiptKey,
                        rejectionReason = dto.rejectionReason,
                        timestamp = dto.timestamp
                    )
                    transactionDao.insert(entity)
                }
            }

            // Sync Agent Accounts
            val agentResponse = api.getAgentAccounts()
            if (agentResponse.isSuccessful) {
                agentResponse.body()?.forEach { dto ->
                    settingsDao.insertAgentAccount(
                        AgentAccountEntity(
                            id = dto.id,
                            label = dto.label,
                            accountNumber = dto.accountNumber,
                            method = dto.method,
                            isActive = dto.isActive == 1
                        )
                    )
                }
            }

            // Sync Notifications
            val notifResponse = api.getNotifications()
            if (notifResponse.isSuccessful) {
                notifResponse.body()?.forEach { dto ->
                    notificationDao.insert(
                        NotificationEntity(
                            id = dto.id,
                            title = dto.title,
                            message = dto.message,
                            isRead = dto.isRead == 1,
                            createdAt = dto.createdAt
                        )
                    )
                }
            }
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
    ): String = withContext(Dispatchers.IO) {
        var reference = generateReference()
        val timestamp = System.currentTimeMillis()

        // Attempt Cloudflare API write
        val remoteCall = runCatching {
            api.createDeposit(
                DepositRequest(
                    amount = amount,
                    playerId = playerId,
                    paymentMethod = paymentMethod,
                    receiptKey = receiptKey
                )
            )
        }

        if (remoteCall.isSuccess && remoteCall.getOrNull()?.isSuccessful == true) {
            val res = remoteCall.getOrNull()!!.body()
            if (res != null) {
                reference = res.reference
            }
        }

        val transaction = TransactionEntity(
            reference = reference,
            type = TransactionType.DEPOSIT,
            status = TransactionStatus.PENDING,
            amount = amount,
            playerId = playerId,
            paymentMethod = paymentMethod,
            receiptKey = receiptKey,
            timestamp = timestamp
        )
        transactionDao.insert(transaction)
        notificationDao.insert(
            NotificationEntity(
                title = "Deposit Received",
                message = "Request $reference for LKR $amount submitted and under review."
            )
        )
        reference
    }

    suspend fun createWithdrawal(
        amount: Long,
        playerId: String,
        securityCode: String,
        bank: String,
        accountNumber: String,
        contactNumber: String
    ): String = withContext(Dispatchers.IO) {
        var reference = generateReference()
        val timestamp = System.currentTimeMillis()

        // Attempt Cloudflare API write
        val remoteCall = runCatching {
            api.createWithdrawal(
                WithdrawalRequest(
                    amount = amount,
                    playerId = playerId,
                    securityCode = securityCode,
                    bank = bank,
                    accountNumber = accountNumber,
                    contactNumber = contactNumber
                )
            )
        }

        if (remoteCall.isSuccess && remoteCall.getOrNull()?.isSuccessful == true) {
            val res = remoteCall.getOrNull()!!.body()
            if (res != null) {
                reference = res.reference
            }
        }

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
            contactNumber = contactNumber,
            timestamp = timestamp
        )
        transactionDao.insert(transaction)
        notificationDao.insert(
            NotificationEntity(
                title = "Withdrawal Received",
                message = "Request $reference for LKR $amount submitted and queued for payout."
            )
        )
        reference
    }

    suspend fun findTransactionByReference(ref: String): TransactionEntity? = withContext(Dispatchers.IO) {
        val trimmed = ref.trim()

        // Check remote Cloudflare API first
        runCatching {
            val response = api.getTransactionByReference(trimmed)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val entity = TransactionEntity(
                    id = dto.id,
                    reference = dto.reference,
                    type = runCatching { TransactionType.valueOf(dto.type) }.getOrDefault(TransactionType.DEPOSIT),
                    status = runCatching { TransactionStatus.valueOf(dto.status) }.getOrDefault(TransactionStatus.PENDING),
                    amount = dto.amount,
                    playerId = dto.playerId,
                    paymentMethod = dto.paymentMethod,
                    securityCode = dto.securityCode,
                    bank = dto.bank,
                    accountNumber = dto.accountNumber,
                    contactNumber = dto.contactNumber,
                    receiptKey = dto.receiptKey,
                    rejectionReason = dto.rejectionReason,
                    timestamp = dto.timestamp
                )
                transactionDao.insert(entity)
                return@withContext entity
            }
        }

        // Fallback to local Room Database cache
        transactionDao.getByReference(trimmed)
    }

    suspend fun updateTransactionStatus(id: Long, status: TransactionStatus, reason: String? = null) = withContext(Dispatchers.IO) {
        // Update on Cloudflare API
        runCatching {
            api.updateTransactionStatus(
                id = id,
                request = AdminUpdateStatusRequest(
                    status = status.name,
                    reason = reason
                )
            )
        }

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

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        runCatching { api.markAllNotificationsAsRead() }
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
