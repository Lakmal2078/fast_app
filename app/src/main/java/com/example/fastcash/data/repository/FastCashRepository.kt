package com.example.fastcash.data.repository

import com.example.fastcash.data.local.AppDatabase
import com.example.fastcash.data.model.*
import com.example.fastcash.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID
import android.content.SharedPreferences
import com.google.gson.Gson
import java.io.IOException
import java.security.MessageDigest
import retrofit2.Response

class FastCashRepository(
    private val db: AppDatabase,
    private val api: FastCashApiService,
    private val pending: SharedPreferences
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

    private fun <T> Response<T>.confirmed(): T =
        if (isSuccessful && body() != null) body()!!
        else throw IOException("ඉල්ලීම තහවුරු කර නැත (HTTP ${code()}).")

    private suspend fun cache(dto: TransactionDto): TransactionEntity =
        transactionDao.cacheByReference(TransactionEntity(
            reference = dto.reference, type = TransactionType.valueOf(dto.type),
            status = TransactionStatus.valueOf(dto.status), amount = dto.amount,
            playerId = dto.playerId, paymentMethod = dto.paymentMethod,
            rejectionReason = dto.rejectionReason, timestamp = dto.timestamp
        ))

    suspend fun syncFromRemote() {
        api.getTransactions().confirmed().forEach { cache(it) }
        api.getAgentAccounts().confirmed().let { accounts ->
            settingsDao.replaceAccounts(accounts.map { AgentAccountEntity(
                id = it.id, label = it.label, accountNumber = it.accountNumber,
                method = it.method, isActive = it.isActive == 1
            ) })
        }
    }

    private suspend fun submit(kind: String, request: Any,
        send: suspend (String) -> Response<TransactionDto>): String = withContext(Dispatchers.IO) {
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(Gson().toJson(request).toByteArray()).joinToString("") { "%02x".format(it) }
        val slot = "$kind-$fingerprint"
        val key = pending.getString(slot, null) ?: UUID.randomUUID().toString().also {
            check(pending.edit().putString(slot, it).commit())
        }
        val saved = send(key).confirmed()
        check(saved.reference.isNotBlank() && saved.id > 0 && saved.type == kind)
        cache(saved)
        check(pending.edit().putString("ack-${saved.reference}", slot).commit())
        saved.reference
    }

    fun acknowledge(reference: String) {
        val slot = pending.getString("ack-$reference", null) ?: return
        pending.edit().remove(slot).remove("ack-$reference").apply()
    }

    suspend fun createDeposit(amount: Long, playerId: String, paymentMethod: String, receiptKey: String?): String {
        val request = DepositRequest(amount, playerId, paymentMethod, receiptKey)
        return submit("DEPOSIT", request) { api.createDeposit(it, request) }
    }

    suspend fun createWithdrawal(amount: Long, playerId: String, securityCode: String, bank: String,
        accountNumber: String, contactNumber: String): String {
        val request = WithdrawalRequest(amount, playerId, securityCode, bank, accountNumber, contactNumber)
        return submit("WITHDRAWAL", request) { api.createWithdrawal(it, request) }
    }

    suspend fun findTransactionByReference(ref: String): TransactionEntity? {
        val response = api.getTransactionByReference(ref.trim().uppercase())
        return if (response.code() == 404) null else cache(response.confirmed())
    }

    suspend fun updateTransactionStatus(id: Long, status: TransactionStatus, reason: String? = null) = withContext(Dispatchers.IO) {
        api.updateTransactionStatus(id, AdminUpdateStatusRequest(status.name, reason)).confirmed()
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
