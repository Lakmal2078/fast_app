package com.example.fastcash.data.local

import androidx.room.*
import com.example.fastcash.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE reference = :ref LIMIT 1")
    suspend fun getByReference(ref: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @androidx.room.Transaction
    suspend fun cacheByReference(transaction: TransactionEntity): TransactionEntity {
        val existing = getByReference(transaction.reference)
        if (existing == null) {
            return transaction.copy(id = insert(transaction.copy(id = 0)))
        }
        val updated = transaction.copy(id = existing.id)
        update(updated)
        return updated
    }

    @Query("UPDATE transactions SET status = :status, rejectionReason = :reason WHERE id = :id")
    suspend fun updateStatus(id: Long, status: TransactionStatus, reason: String? = null)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getAppSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAppSettings(settings: AppSettingsEntity)

    @Query("SELECT * FROM agent_accounts WHERE isActive = 1")
    fun getActiveAgentAccounts(): Flow<List<AgentAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentAccount(account: AgentAccountEntity)

    @Query("DELETE FROM agent_accounts")
    suspend fun clearAccounts()

    @androidx.room.Transaction
    suspend fun replaceAccounts(accounts: List<AgentAccountEntity>) {
        clearAccounts()
        accounts.forEach { insertAgentAccount(it) }
    }

    @Query("SELECT * FROM recon_rows ORDER BY createdAt DESC")
    fun getAllReconRows(): Flow<List<ReconRowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconRow(row: ReconRowEntity): Long

    @Update
    suspend fun updateReconRow(row: ReconRowEntity)
}
