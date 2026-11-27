package com.example.fastcash.data.repository

import android.content.Context
import com.example.fastcash.R
import com.example.fastcash.data.model.AgentDestinationAccount
import com.example.fastcash.data.model.FirestoreAppConfig
import com.example.fastcash.data.model.FirestoreNotification
import com.example.fastcash.data.model.FirestoreTransaction
import com.example.fastcash.data.model.FirestoreUserProfile
import com.example.fastcash.data.model.SavedBankPreset
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.data.remote.OperationType
import com.example.fastcash.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreFastCashRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    suspend fun checkIsAdmin(uid: String, email: String?, emailVerified: Boolean): Boolean {
        if (emailVerified && email.equals(ADMIN_EMAIL, ignoreCase = true)) {
            return true
        }
        return try {
            db.collection("admins").document(uid).get().await().exists()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    fun observePlayerTransactions(userId: String): Flow<List<FirestoreTransaction>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "transactions"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllTransactionsForAdmin(): Flow<List<FirestoreTransaction>> = flow {
        val uid = requireUserId()
        val path = "transactions"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<FirestoreNotification>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            FirestoreUserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeAppConfig(): Flow<FirestoreAppConfig> = flow {
        val uid = requireUserId()
        val path = "app_config/settings"
        emitAll(
            db.collection("app_config").document("settings")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            FirestoreAppConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: FirestoreAppConfig()
                    } else {
                        FirestoreAppConfig()
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    emit(FirestoreAppConfig())
                }
        )
    }

    suspend fun createDeposit(
        amount: Long,
        playerId: String,
        paymentMethod: String,
        destination: AgentDestinationAccount?,
        receiptKey: String?,
        slipImageBase64: String? = null
    ): Result<String> {
        var docPath = "transactions"
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("User must be signed in with Google before submitting.")
            val uid = user.uid
            require(amount in 1000L..500000L) { "Amount must be between LKR 1,000 and 500,000" }
            require(playerId.trim().length in 4..64) { "Invalid 1xBet Player ID" }
            require(paymentMethod.trim().length in 1..64) { "Invalid payment method" }
            val cleanSlip = slipImageBase64?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Deposit slip photo is required")
            require(cleanSlip.length <= 350000) { "Slip image exceeds allowed size" }

            val reference = "TXN-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
            val txDoc = db.collection("transactions").document()
            docPath = txDoc.path

            val payload = mapOf(
                "userId" to uid,
                "userEmail" to user.email?.take(160)?.takeIf { it.isNotBlank() },
                "userName" to user.displayName?.take(120)?.takeIf { it.isNotBlank() },
                "reference" to reference,
                "type" to "DEPOSIT",
                "status" to "PENDING",
                "amount" to amount,
                "playerId" to playerId.trim(),
                "paymentMethod" to paymentMethod.trim(),
                "agentAccountLabel" to destination?.displayLabel?.take(140),
                "agentAccountNumber" to destination?.accountNumber?.take(64),
                "receiptKey" to receiptKey?.trim()?.take(128)?.takeIf { it.isNotBlank() },
                "slipImageBase64" to cleanSlip,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            txDoc.set(payload).await()

            val notifDoc = db.collection("users").document(uid)
                .collection("notifications").document()
            val notifPayload = mapOf(
                "title" to "Deposit Request Sent",
                "message" to "$reference · LKR $amount submitted. Please send WhatsApp confirmation to agent.",
                "type" to "SUBMITTED",
                "transactionRef" to reference,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            notifDoc.set(notifPayload).await()

            Result.success(reference)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createWithdrawal(
        amount: Long,
        playerId: String,
        securityCode: String,
        bank: String,
        accountNumber: String,
        contactNumber: String
    ): Result<String> {
        var docPath = "transactions"
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("User must be signed in with Google before submitting.")
            val uid = user.uid
            require(amount in 1000L..500000L) { "Amount must be between LKR 1,000 and 500,000" }
            require(playerId.trim().length in 4..64) { "Invalid 1xBet Player ID" }
            require(securityCode.trim().length in 1..128) { "Security code is required" }
            require(bank.trim().length in 1..100) { "Bank is required" }
            require(accountNumber.trim().length in 6..64) { "Valid bank account number is required" }

            val reference = "TXN-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
            val txDoc = db.collection("transactions").document()
            docPath = txDoc.path

            val payload = mapOf(
                "userId" to uid,
                "userEmail" to user.email?.take(160)?.takeIf { it.isNotBlank() },
                "userName" to user.displayName?.take(120)?.takeIf { it.isNotBlank() },
                "reference" to reference,
                "type" to "WITHDRAWAL",
                "status" to "PENDING",
                "amount" to amount,
                "playerId" to playerId.trim(),
                "paymentMethod" to "Bank payout",
                "securityCode" to securityCode.trim(),
                "bank" to bank.trim(),
                "accountNumber" to accountNumber.trim(),
                "contactNumber" to contactNumber.trim().take(32).takeIf { it.isNotBlank() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            txDoc.set(payload).await()

            val notifDoc = db.collection("users").document(uid)
                .collection("notifications").document()
            val notifPayload = mapOf(
                "title" to "Withdrawal Request Sent",
                "message" to "$reference · LKR $amount submitted to $bank.",
                "type" to "SUBMITTED",
                "transactionRef" to reference,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            notifDoc.set(notifPayload).await()

            Result.success(reference)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun findTransactionByReference(
        userId: String,
        isAdmin: Boolean,
        reference: String
    ): Result<FirestoreTransaction?> {
        val path = "transactions"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanRef = reference.trim().uppercase()
            val query = if (isAdmin) {
                db.collection(path)
            } else {
                db.collection(path).whereEqualTo("userId", uid)
            }
            val matches = query.get().await().toObjects(
                FirestoreTransaction::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(matches.firstOrNull { it.reference.equals(cleanRef, ignoreCase = true) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun updateTransactionAndNotifyPlayer(
        tx: FirestoreTransaction,
        newStatus: TransactionStatus,
        agentNote: String? = null,
        rejectionReason: String? = null
    ): Result<Unit> {
        val path = "transactions/${tx.id}"
        return try {
            requireUserId()
            require(tx.id.isNotBlank()) { "Transaction ID cannot be blank" }
            require(tx.userId.isNotBlank()) { "Transaction owner ID cannot be blank" }

            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            agentNote?.trim()?.take(300)?.takeIf { it.isNotBlank() }?.let {
                updates["agentNote"] = it
            }
            rejectionReason?.trim()?.take(300)?.takeIf { it.isNotBlank() }?.let {
                updates["rejectionReason"] = it
            }

            db.collection("transactions").document(tx.id).update(updates).await()

            val isDeposit = tx.type == "DEPOSIT"
            val title = when (newStatus) {
                TransactionStatus.COMPLETED ->
                    if (isDeposit) "Deposit Credited via MobCash" else "Withdrawal Payout Sent"
                TransactionStatus.PROCESSING ->
                    if (isDeposit) "Deposit Verifying (MobCash)" else "Withdrawal Processing"
                TransactionStatus.REJECTED -> "Request Rejected"
                TransactionStatus.PENDING -> "Request Updated"
            }

            val detailText = when (newStatus) {
                TransactionStatus.COMPLETED ->
                    agentNote?.takeIf { it.isNotBlank() }
                        ?: if (isDeposit) {
                            "LKR ${tx.amount} has been credited to 1xBet ID ${tx.playerId}."
                        } else {
                            "LKR ${tx.amount} has been transferred to ${tx.bank ?: "your bank"} (${tx.accountNumber ?: ""})."
                        }
                TransactionStatus.PROCESSING ->
                    agentNote?.takeIf { it.isNotBlank() }
                        ?: "Agent is processing your ${tx.type.lowercase()} of LKR ${tx.amount}."
                TransactionStatus.REJECTED ->
                    rejectionReason?.takeIf { it.isNotBlank() } ?: "Rejected by agent."
                TransactionStatus.PENDING -> "Status returned to pending."
            }

            val notifDoc = db.collection("users").document(tx.userId)
                .collection("notifications").document()
            val notifPayload = mapOf(
                "title" to title,
                "message" to "${tx.reference} · $detailText",
                "type" to newStatus.name,
                "transactionRef" to tx.reference,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            notifDoc.set(notifPayload).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(userId: String, notificationId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notificationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            require(notificationId.isNotBlank()) { "Notification ID required" }
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .update(mapOf("read" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(userId: String, unreadIds: List<String>): Result<Unit> {
        val path = "users/$userId/notifications"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            if (unreadIds.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            unreadIds.forEach { notifId ->
                if (notifId.isNotBlank()) {
                    val ref = db.collection("users").document(uid)
                        .collection("notifications").document(notifId)
                    batch.update(ref, mapOf("read" to true))
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(userId: String, name: String, playerId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanName = name.trim().take(100)
            val cleanPlayerId = playerId.trim().take(64)
            require(cleanName.length in 1..100) { "Name must be 1 to 100 characters" }
            require(cleanPlayerId.length in 4..64) { "Player ID must be at least 4 characters" }

            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(FirestoreUserProfile::class.java)
                val updatedIds = buildUpdatedPlayerIds(existing?.savedPlayerIds.orEmpty(), cleanPlayerId)
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "playerId" to cleanPlayerId,
                        "savedPlayerIds" to updatedIds,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "playerId" to cleanPlayerId,
                        "savedPlayerIds" to listOf(cleanPlayerId),
                        "savedBankPresets" to emptyList<Map<String, Any>>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun savePlayerIdPreset(
        userId: String,
        defaultName: String,
        newPlayerId: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanId = newPlayerId.trim().take(64)
            require(cleanId.length in 4..64) { "Player ID must be at least 4 characters" }
            val fallbackName = defaultName.trim().take(100).ifBlank { "Player" }

            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(FirestoreUserProfile::class.java)
                val updatedIds = buildUpdatedPlayerIds(existing?.savedPlayerIds.orEmpty(), cleanId)
                val nextDefaultId = existing?.playerId?.takeIf { it.isNotBlank() } ?: cleanId
                docRef.update(
                    mapOf(
                        "playerId" to nextDefaultId,
                        "savedPlayerIds" to updatedIds,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to fallbackName,
                        "playerId" to cleanId,
                        "savedPlayerIds" to listOf(cleanId),
                        "savedBankPresets" to emptyList<Map<String, Any>>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removePlayerIdPreset(userId: String, playerIdToRemove: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return Result.success(Unit)
            val existing = snapshot.toObject(FirestoreUserProfile::class.java)
            val updatedIds = existing?.savedPlayerIds.orEmpty().filterNot { it == playerIdToRemove }
            docRef.update(
                mapOf(
                    "savedPlayerIds" to updatedIds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBankPreset(
        userId: String,
        defaultName: String,
        defaultPlayerId: String,
        bank: String,
        accountNumber: String,
        contactNumber: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val cleanBank = bank.trim().take(100)
            val cleanAcc = accountNumber.trim().take(64)
            val cleanContact = contactNumber?.trim()?.take(32)?.takeIf { it.isNotBlank() }
            require(cleanBank.isNotEmpty()) { "Select a bank first" }
            require(cleanAcc.length >= 6) { "Enter a valid bank account number (at least 6 digits)" }
            val fallbackName = defaultName.trim().take(100).ifBlank { "Player" }
            val fallbackPlayerId = defaultPlayerId.trim().take(64)

            val newPreset = SavedBankPreset(
                id = UUID.randomUUID().toString().replace("-", "").take(12),
                bank = cleanBank,
                accountNumber = cleanAcc,
                contactNumber = cleanContact
            )

            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(FirestoreUserProfile::class.java)
                val updatedPresets = buildUpdatedBankPresets(existing?.savedBankPresets.orEmpty(), newPreset)
                docRef.update(
                    mapOf(
                        "savedBankPresets" to updatedPresets.map { it.toFirestoreMap() },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val initialIds = if (fallbackPlayerId.length >= 4) listOf(fallbackPlayerId) else emptyList()
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to fallbackName,
                        "playerId" to fallbackPlayerId,
                        "savedPlayerIds" to initialIds,
                        "savedBankPresets" to listOf(newPreset.toFirestoreMap()),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeBankPreset(userId: String, presetId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return Result.success(Unit)
            val existing = snapshot.toObject(FirestoreUserProfile::class.java)
            val updatedPresets = existing?.savedBankPresets.orEmpty().filterNot { it.id == presetId }
            docRef.update(
                mapOf(
                    "savedBankPresets" to updatedPresets.map { it.toFirestoreMap() },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun buildUpdatedPlayerIds(existing: List<String>, newId: String): List<String> =
        (listOf(newId) + existing.filterNot { it == newId }).take(10)

    private fun buildUpdatedBankPresets(
        existing: List<SavedBankPreset>,
        newPreset: SavedBankPreset
    ): List<SavedBankPreset> {
        val deduplicated = existing.filterNot {
            it.bank.equals(newPreset.bank, ignoreCase = true) &&
                it.accountNumber == newPreset.accountNumber
        }
        return (listOf(newPreset) + deduplicated).take(10)
    }

    private fun SavedBankPreset.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "bank" to bank,
        "accountNumber" to accountNumber,
        "contactNumber" to contactNumber?.takeIf { it.isNotBlank() }
    ).filterValues { it != null } as Map<String, Any>

    suspend fun saveAppConfig(
        whatsappNumber: String,
        minTransaction: Long,
        maxTransaction: Long,
        promoCode: String
    ): Result<Unit> {
        val path = "app_config/settings"
        return try {
            requireUserId()
            val cleanWa = whatsappNumber.trim()
            val cleanPromo = promoCode.trim()
            require(cleanWa.length in 8..24) { "Invalid WhatsApp number" }
            require(minTransaction in 100L..500000L) { "Invalid minimum transaction" }
            require(maxTransaction >= minTransaction && maxTransaction <= 10000000L) { "Invalid maximum transaction" }
            require(cleanPromo.length in 1..32) { "Invalid promo code" }

            db.collection("app_config").document("settings").set(
                mapOf(
                    "whatsappNumber" to cleanWa,
                    "minTransaction" to minTransaction,
                    "maxTransaction" to maxTransaction,
                    "promoCode" to cleanPromo,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val ADMIN_EMAIL = "lakmalsujith25@gmail.com"
    }
}
