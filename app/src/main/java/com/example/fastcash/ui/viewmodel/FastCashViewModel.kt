package com.example.fastcash.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fastcash.data.model.AgentDestinationAccount
import com.example.fastcash.data.model.DefaultAgentAccounts
import com.example.fastcash.data.model.FirestoreAppConfig
import com.example.fastcash.data.model.FirestoreNotification
import com.example.fastcash.data.model.FirestoreTransaction
import com.example.fastcash.data.model.FirestoreUserProfile
import com.example.fastcash.data.model.SavedBankPreset
import com.example.fastcash.data.model.TransactionEntity
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.data.repository.FirestoreFastCashRepository
import com.example.fastcash.ui.components.ProcessedSlip
import com.example.fastcash.ui.components.SlipOcrResult
import com.example.fastcash.ui.components.WhatsAppHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    HOME,
    DEPOSIT,
    WITHDRAW,
    TRACK,
    ADMIN,
    SUPPORT
}

data class LocalizedContent(
    val heroKicker: String,
    val heroTitle: String,
    val heroText: String,
    val deposit: String,
    val withdraw: String,
    val track: String,
    val live: String,
    val secure: String,
    val steps: List<String>,
    val support: String,
    val responsible: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class FastCashViewModel(
    private val repository: FirestoreFastCashRepository,
    val currentUserId: String,
    val currentUserEmail: String?,
    val currentUserName: String?,
    private val isEmailVerified: Boolean
) : ViewModel() {

    val syncError = MutableStateFlow<String?>(null)

    // Navigation & Global UI State
    val currentScreen = MutableStateFlow(ScreenDestination.HOME)
    val selectedLanguage = MutableStateFlow("si")
    val isNotificationsSheetOpen = MutableStateFlow(false)
    val isAdmin = MutableStateFlow(false)
    val isAdminMode = MutableStateFlow(false)

    // Firestore App Config
    val appConfig: StateFlow<FirestoreAppConfig> = repository.observeAppConfig()
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing app config", firestoreError ?: error)
            emit(FirestoreAppConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FirestoreAppConfig())

    // Agent Accounts (synced from Firestore App Config, falling back to built-in defaults)
    val agentAccounts: StateFlow<List<AgentDestinationAccount>> = appConfig
        .map { it.effectiveAgentAccounts() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DefaultAgentAccounts.accounts)
    val selectedAgentAccount = MutableStateFlow(DefaultAgentAccounts.accounts.first())

    // Player Transactions (scoped by userId)
    val playerFirestoreTransactions: StateFlow<List<FirestoreTransaction>> =
        repository.observePlayerTransactions(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing player transactions", root)
                syncError.value = root.localizedMessage ?: "Failed to sync transactions."
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = playerFirestoreTransactions
        .map { list -> list.map { it.toEntity() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // Admin Transactions (only observed when verified as admin)
    val adminFirestoreTransactions: StateFlow<List<FirestoreTransaction>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) {
                flowOf(emptyList())
            } else {
                repository.observeAllTransactionsForAdmin()
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        Log.w(TAG, "Error observing admin queue", firestoreError ?: error)
                        emit(emptyList())
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // Player Notifications (real-time from Firestore)
    val allNotifications: StateFlow<List<FirestoreNotification>> =
        repository.observeNotifications(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing notifications", firestoreError ?: error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = allNotifications
        .map { list -> list.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    val latestUnreadAgentAlert: StateFlow<FirestoreNotification?> = allNotifications
        .map { list ->
            list.firstOrNull { !it.read && it.type != "SUBMITTED" }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val userProfile: StateFlow<FirestoreUserProfile?> =
        repository.observeUserProfile(currentUserId)
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing user profile", firestoreError ?: error)
                emit(null)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val savedPlayerIds: StateFlow<List<String>> = userProfile
        .map { prof ->
            val ids = prof?.savedPlayerIds.orEmpty().toMutableList()
            val primary = prof?.playerId?.trim().orEmpty()
            if (primary.length >= 4 && primary !in ids) {
                ids.add(0, primary)
            }
            ids.distinct().take(10)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val savedBankPresets: StateFlow<List<SavedBankPreset>> = userProfile
        .map { it?.savedBankPresets.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // Language Content
    val content: StateFlow<LocalizedContent> = selectedLanguage.map { lang ->
        when (lang) {
            "si" -> LocalizedContent(
                heroKicker = "නිල නියෝජිත මෙහෙයුම් // ශ්‍රී ලංකාව",
                heroTitle = "විශ්වාසය වගේ වේගයෙන් මුදල් ගමන් කරයි.",
                heroText = "ඔබගේ 1xBet wallet එක සහ Fast Cash නියෝජිතයන් අතර පැහැදිලි, track කළ හැකි මාර්ගයක්. එක් වරක් submit කර WhatsApp වෙත යොමු කරන්න.",
                deposit = "Deposit ආරම්භ කරන්න",
                withdraw = "Withdrawal ඉල්ලන්න",
                track = "Request track කරන්න",
                live = "නියෝජිතයන් online",
                secure = "ආරක්ෂාව මුලින්",
                steps = listOf("ගිණුම තෝරා මුදල් දමන්න", "විස්තර WhatsApp වෙත යවන්න", "MobCash තහවුරු කිරීම බලන්න"),
                support = "මිනිස් සහායක් අවශ්‍යද? සේවා වේලාවන්හි WhatsApp සහාය ඇත.",
                responsible = "වයස 18+ සඳහා පමණි. සූදුව විනෝදාස්වාදයක් පමණක් විය යුතුය. ණයට ගත් මුදල් භාවිතා නොකරන්න."
            )
            "ta" -> LocalizedContent(
                heroKicker = "அதிகாரப்பூர்வ முகவர் சேவை // இலங்கை",
                heroTitle = "நம்பிக்கையின் வேகத்தில் பணம் நகரும்.",
                heroText = "உங்கள் 1xBet wallet மற்றும் Fast Cash முகவர்களுக்கு இடையில் தெளிவான, track செய்யக்கூடிய பாலம்.",
                deposit = "Deposit தொடங்கு",
                withdraw = "Withdrawal கேள்",
                track = "Request track செய்",
                live = "முகவர்கள் online",
                secure = "பாதுகாப்பு முதலில்",
                steps = listOf("கணக்கை தேர்ந்தெடுத்து அனுப்பு", "WhatsApp மூலம் உறுதிப்படுத்து", "MobCash நிலையைப் பார்"),
                support = "மனித உதவி வேண்டுமா? சேவை நேரத்தில் WhatsApp உதவி உள்ளது.",
                responsible = "18+ மட்டும். சூதாட்டம் பொழுதுபோக்காக இருக்க வேண்டும். கடன் பணத்தை பயன்படுத்த வேண்டாம்."
            )
            else -> LocalizedContent(
                heroKicker = "OFFICIAL AGENT OPERATIONS // SRI LANKA",
                heroTitle = "Your money moves at the speed of trust.",
                heroText = "A clear, trackable bridge between your 1xBet wallet and Fast Cash agents. Copy bank details, submit once to WhatsApp, and receive live MobCash confirmations.",
                deposit = "Start a deposit",
                withdraw = "Request withdrawal",
                track = "Track request",
                live = "AGENTS ONLINE",
                secure = "Protected by design",
                steps = listOf("Copy bank or iPay details", "Submit & send to WhatsApp", "Get live MobCash app alert"),
                support = "Need a human? WhatsApp support is available during service hours.",
                responsible = "18+ only. Gambling should be entertainment. Never use borrowed money, and stop if it stops being fun."
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        LocalizedContent(
            heroKicker = "නිල නියෝජිත මෙහෙයුම් // ශ්‍රී ලංකාව",
            heroTitle = "විශ්වාසය වගේ වේගයෙන් මුදල් ගමන් කරයි.",
            heroText = "ඔබගේ 1xBet wallet එක සහ Fast Cash නියෝජිතයන් අතර පැහැදිලි, track කළ හැකි මාර්ගයක්. එක් වරක් submit කර WhatsApp වෙත යොමු කරන්න.",
            deposit = "Deposit ආරම්භ කරන්න",
            withdraw = "Withdrawal ඉල්ලන්න",
            track = "Request track කරන්න",
            live = "නියෝජිතයන් online",
            secure = "ආරක්ෂාව මුලින්",
            steps = listOf("ගිණුම තෝරා මුදල් දමන්න", "විස්තර WhatsApp වෙත යවන්න", "MobCash තහවුරු කිරීම බලන්න"),
            support = "මිනිස් සහායක් අවශ්‍යද? සේවා වේලාවන්හි WhatsApp සහාය ඇත.",
            responsible = "වයස 18+ සඳහා පමණි. සූදුව විනෝදාස්වාදයක් පමණක් විය යුතුය. ණයට ගත් මුදල් භාවිතා නොකරන්න."
        )
    )

    // Deposit State
    val depositAmount = MutableStateFlow("")
    val depositPlayerId = MutableStateFlow("")
    val depositPaymentMethod = MutableStateFlow("Bank transfer")
    val depositReceiptRef = MutableStateFlow("")
    val depositSlipBase64 = MutableStateFlow<String?>(null)
    val depositSlipShareUri = MutableStateFlow<Uri?>(null)
    val depositSlipSizeKb = MutableStateFlow<Int?>(null)
    val depositOcrSummary = MutableStateFlow<String?>(null)
    val depositOcrMatched = MutableStateFlow(false)
    val depositOcrDetails = MutableStateFlow<SlipOcrResult?>(null)
    val depositIsProcessingSlip = MutableStateFlow(false)
    val depositIsSubmitting = MutableStateFlow(false)
    val depositSuccessRef = MutableStateFlow<String?>(null)
    val depositWhatsappMessage = MutableStateFlow<String?>(null)
    val depositSubmittedSlipUri = MutableStateFlow<Uri?>(null)
    val depositError = MutableStateFlow<String?>(null)
    val isSavingPreset = MutableStateFlow(false)

    // Withdrawal State
    val withdrawAmount = MutableStateFlow("")
    val withdrawPlayerId = MutableStateFlow("")
    val withdrawSecurityCode = MutableStateFlow("")
    val withdrawBank = MutableStateFlow("Bank of Ceylon (BOC)")
    val withdrawAccountNumber = MutableStateFlow("")
    val withdrawAccountName = MutableStateFlow("")
    val withdrawContactNumber = MutableStateFlow("")
    val withdrawIsSubmitting = MutableStateFlow(false)
    val withdrawSuccessRef = MutableStateFlow<String?>(null)
    val withdrawWhatsappMessage = MutableStateFlow<String?>(null)
    val withdrawError = MutableStateFlow<String?>(null)

    // Auto-launch WhatsApp one-shot message (consumed by UI)
    val pendingWhatsappAutoLaunch = MutableStateFlow<String?>(null)

    // Tracking State
    val trackSearchQuery = MutableStateFlow("")
    val trackedResult = MutableStateFlow<FirestoreTransaction?>(null)
    val isTrackSearched = MutableStateFlow(false)
    val trackingMessage = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    // Profile State
    val profileName = MutableStateFlow("")
    val profilePlayerId = MutableStateFlow("")
    val isSavingProfile = MutableStateFlow(false)
    val profileMessage = MutableStateFlow<String?>(null)

    // Admin State
    val adminSearchQuery = MutableStateFlow("")
    val adminStatusFilter = MutableStateFlow("ALL")
    val selectedAdminTx = MutableStateFlow<FirestoreTransaction?>(null)
    val adminAgentNote = MutableStateFlow("")
    val adminRejectionReason = MutableStateFlow("")
    val adminActionError = MutableStateFlow<String?>(null)
    val isUpdatingAdminTx = MutableStateFlow(false)

    val adminWhatsapp = MutableStateFlow("+94765865387")
    val adminMinAmount = MutableStateFlow("1000")
    val adminMaxAmount = MutableStateFlow("500000")
    val adminPromoCode = MutableStateFlow("VGSL")
    val adminAgentAccounts = MutableStateFlow(DefaultAgentAccounts.accounts)

    init {
        viewModelScope.launch {
            val adminVerified = repository.checkIsAdmin(currentUserId, currentUserEmail, isEmailVerified)
            isAdmin.value = adminVerified
            isAdminMode.value = adminVerified
        }
        viewModelScope.launch {
            userProfile.collect { prof ->
                if (prof != null) {
                    if (profileName.value.isBlank()) profileName.value = prof.name
                    if (profilePlayerId.value.isBlank()) {
                        profilePlayerId.value = prof.playerId
                        if (depositPlayerId.value.isBlank()) depositPlayerId.value = prof.playerId
                        if (withdrawPlayerId.value.isBlank()) withdrawPlayerId.value = prof.playerId
                    }
                }
            }
        }
        viewModelScope.launch {
            appConfig.collect { cfg ->
                adminWhatsapp.value = cfg.whatsappNumber
                adminMinAmount.value = cfg.minTransaction.toString()
                adminMaxAmount.value = cfg.maxTransaction.toString()
                adminPromoCode.value = cfg.promoCode
                val effective = cfg.effectiveAgentAccounts()
                adminAgentAccounts.value = effective
                val currentId = selectedAgentAccount.value.id
                selectedAgentAccount.value = effective.firstOrNull { it.id == currentId } ?: effective.first()
            }
        }
    }

    fun setLanguage(lang: String) {
        selectedLanguage.value = lang
    }

    fun navigateTo(screen: ScreenDestination) {
        if (screen == ScreenDestination.ADMIN && !isAdmin.value) return
        currentScreen.value = screen
    }

    fun toggleAdminMode() {
        if (!isAdmin.value) return
        val next = !isAdminMode.value
        isAdminMode.value = next
        if (next) {
            currentScreen.value = ScreenDestination.ADMIN
        } else if (currentScreen.value == ScreenDestination.ADMIN) {
            currentScreen.value = ScreenDestination.HOME
        }
    }

    fun selectAgentAccount(account: AgentDestinationAccount) {
        selectedAgentAccount.value = account
        depositPaymentMethod.value = account.method
    }

    fun consumePendingWhatsappLaunch() {
        pendingWhatsappAutoLaunch.value = null
    }

    fun setProcessedSlip(slip: ProcessedSlip, ocrResult: SlipOcrResult? = null) {
        depositOcrDetails.value = ocrResult
        depositSlipBase64.value = slip.base64
        depositSlipShareUri.value = slip.shareUri
        depositSlipSizeKb.value = slip.sizeKb
        depositError.value = null

        if (ocrResult != null) {
            ocrResult.detectedAccount?.let { selectAgentAccount(it) }
                ?: ocrResult.detectedMethod?.let { depositPaymentMethod.value = it }
            ocrResult.detectedAmount?.let { depositAmount.value = it.toString() }
            ocrResult.detectedReference?.let { depositReceiptRef.value = it }

            val detectedParts = buildList {
                ocrResult.detectedAmount?.let { add("LKR $it") }
                ocrResult.detectedAccount?.let { add(it.bankName) }
                    ?: ocrResult.detectedMethod?.let { add(it) }
                ocrResult.detectedReference?.let { add("Ref $it") }
            }
            if (detectedParts.isNotEmpty()) {
                depositOcrMatched.value = true
                depositOcrSummary.value = if (selectedLanguage.value == "si") {
                    "ස්ලිප් එකෙන් Auto-fill කළා: ${detectedParts.joinToString(" · ")}"
                } else {
                    "Auto-filled from slip: ${detectedParts.joinToString(" · ")}"
                }
            } else {
                depositOcrMatched.value = false
                depositOcrSummary.value = if (selectedLanguage.value == "si") {
                    "ස්ලිප් එක අමුණා ඇත. මුදල සහ ගිණුම පරීක්ෂා කරන්න."
                } else {
                    "Slip attached. Please verify amount & bank details below."
                }
            }
        } else {
            depositOcrMatched.value = false
            depositOcrSummary.value = null
        }
    }

    fun clearSelectedSlip() {
        depositOcrDetails.value = null
        depositSlipBase64.value = null
        depositSlipShareUri.value = null
        depositSlipSizeKb.value = null
        depositOcrSummary.value = null
        depositOcrMatched.value = false
        depositReceiptRef.value = ""
    }

    fun submitDeposit() {
        if (depositIsSubmitting.value || depositIsProcessingSlip.value) return
        val slipBase64 = depositSlipBase64.value
        val slipUri = depositSlipShareUri.value
        if (slipBase64.isNullOrBlank()) {
            depositError.value = if (selectedLanguage.value == "si") {
                "කරුණාකර බැංකු ස්ලිප් ෆොටෝ එක upload කරන්න."
            } else {
                "Please upload your deposit slip photo before submitting."
            }
            return
        }
        val amount = depositAmount.value.trim().toLongOrNull()
        val minAmt = appConfig.value.minTransaction
        val maxAmt = appConfig.value.maxTransaction
        if (amount == null || amount < minAmt || amount > maxAmt) {
            depositError.value = if (selectedLanguage.value == "si") {
                "මුදල රු. $minAmt සහ $maxAmt අතර විය යුතුයි."
            } else {
                "Amount must be between LKR $minAmt and $maxAmt"
            }
            return
        }
        val cleanPlayerId = depositPlayerId.value.trim()
        if (cleanPlayerId.length < 4) {
            depositError.value = if (selectedLanguage.value == "si") {
                "වලංගු ක්‍රීඩක අංකයක් ඇතුළත් කරන්න."
            } else {
                "Please enter a valid 1xBet Player ID"
            }
            return
        }

        val destination = selectedAgentAccount.value
        val method = depositPaymentMethod.value
        val receipt = depositReceiptRef.value.trim().ifBlank { null }

        depositIsSubmitting.value = true
        depositError.value = null
        viewModelScope.launch {
            val result = repository.createDeposit(
                amount = amount,
                playerId = cleanPlayerId,
                paymentMethod = method,
                destination = destination,
                receiptKey = receipt,
                slipImageBase64 = slipBase64
            )
            result.fold(
                onSuccess = { ref ->
                    if (cleanPlayerId !in savedPlayerIds.value) {
                        savePlayerIdPreset(cleanPlayerId)
                    }
                    val waMsg = WhatsAppHelper.buildDepositMessage(
                        reference = ref,
                        amount = amount,
                        playerId = cleanPlayerId,
                        destination = destination,
                        paymentMethod = method,
                        receiptRef = receipt,
                        customerName = profileName.value.ifBlank { currentUserName },
                        customerEmail = currentUserEmail,
                        hasSlipImage = true,
                        ocr = depositOcrDetails.value
                    )
                    depositSuccessRef.value = ref
                    depositWhatsappMessage.value = waMsg
                    depositSubmittedSlipUri.value = slipUri
                    // The deposit screen alone shares the slip and caption once.
                    depositAmount.value = ""
                    depositReceiptRef.value = ""
                    clearSelectedSlip()
                },
                onFailure = { e ->
                    depositError.value = e.localizedMessage ?: "Failed to submit deposit request"
                }
            )
            depositIsSubmitting.value = false
        }
    }

    fun resetDepositSuccess() {
        depositSuccessRef.value = null
        depositWhatsappMessage.value = null
        depositSubmittedSlipUri.value = null
    }

    fun submitWithdrawal() {
        if (withdrawIsSubmitting.value) return
        val amount = withdrawAmount.value.trim().toLongOrNull()
        val minAmt = appConfig.value.minTransaction
        val maxAmt = appConfig.value.maxTransaction
        if (amount == null || amount < minAmt || amount > maxAmt) {
            withdrawError.value = if (selectedLanguage.value == "si") {
                "මුදල රු. $minAmt සහ $maxAmt අතර විය යුතුයි."
            } else {
                "Amount must be between LKR $minAmt and $maxAmt"
            }
            return
        }
        val cleanPlayerId = withdrawPlayerId.value.trim()
        if (cleanPlayerId.length < 4) {
            withdrawError.value = if (selectedLanguage.value == "si") {
                "වලංගු ක්‍රීඩක අංකයක් ඇතුළත් කරන්න."
            } else {
                "Please enter a valid 1xBet Player ID"
            }
            return
        }
        val cleanCode = withdrawSecurityCode.value.trim()
        if (cleanCode.isBlank()) {
            withdrawError.value = if (selectedLanguage.value == "si") {
                "1xBet Cash මුදල් ලබාගැනීමේ ආරක්ෂක කේතය ඇතුළත් කරන්න."
            } else {
                "Please enter the 1xBet Cash withdrawal security code"
            }
            return
        }
        val cleanAccount = withdrawAccountNumber.value.trim()
        if (cleanAccount.length < 6) {
            withdrawError.value = if (selectedLanguage.value == "si") {
                "වලංගු බැංකු ගිණුම් අංකයක් ඇතුළත් කරන්න."
            } else {
                "Please enter a valid bank account number"
            }
            return
        }
        val cleanAccountName = withdrawAccountName.value.trim()
        if (cleanAccountName.length < 2) {
            withdrawError.value = if (selectedLanguage.value == "si") {
                "බැංකු ගිණුම් හිමියාගේ නම ඇතුළත් කරන්න."
            } else {
                "Please enter the bank account holder name"
            }
            return
        }
        val bankName = withdrawBank.value
        val contact = withdrawContactNumber.value.trim()

        withdrawIsSubmitting.value = true
        withdrawError.value = null
        viewModelScope.launch {
            val result = repository.createWithdrawal(
                amount = amount,
                playerId = cleanPlayerId,
                securityCode = cleanCode,
                bank = bankName,
                accountNumber = cleanAccount,
                accountName = cleanAccountName,
                contactNumber = contact
            )
            result.fold(
                onSuccess = { ref ->
                    if (cleanPlayerId !in savedPlayerIds.value) {
                        savePlayerIdPreset(cleanPlayerId)
                    }
                    if (savedBankPresets.value.none {
                            it.bank.equals(bankName, ignoreCase = true) && it.accountNumber == cleanAccount
                        }
                    ) {
                        saveBankPresetInternal(bankName, cleanAccount, cleanAccountName, contact.ifBlank { null })
                    }
                    val waMsg = WhatsAppHelper.buildWithdrawalMessage(
                        reference = ref,
                        amount = amount,
                        playerId = cleanPlayerId,
                        securityCode = cleanCode,
                        bank = bankName,
                        accountNumber = cleanAccount,
                        accountName = cleanAccountName,
                        contactNumber = contact.ifBlank { null },
                        customerName = profileName.value.ifBlank { currentUserName },
                        customerEmail = currentUserEmail
                    )
                    withdrawSuccessRef.value = ref
                    withdrawWhatsappMessage.value = waMsg
                    pendingWhatsappAutoLaunch.value = waMsg
                    withdrawAmount.value = ""
                    withdrawSecurityCode.value = ""
                    withdrawAccountNumber.value = ""
                    withdrawAccountName.value = ""
                    withdrawContactNumber.value = ""
                },
                onFailure = { e ->
                    withdrawError.value = e.localizedMessage ?: "Failed to submit withdrawal request"
                }
            )
            withdrawIsSubmitting.value = false
        }
    }

    fun savePlayerIdPreset(rawPlayerId: String) {
        val cleanId = rawPlayerId.trim()
        if (cleanId.length < 4) return
        viewModelScope.launch {
            isSavingPreset.value = true
            repository.savePlayerIdPreset(
                userId = currentUserId,
                defaultName = profileName.value.ifBlank { currentUserName ?: "Player" },
                newPlayerId = cleanId
            )
            isSavingPreset.value = false
        }
    }

    fun removePlayerIdPreset(playerIdToRemove: String) {
        viewModelScope.launch {
            repository.removePlayerIdPreset(currentUserId, playerIdToRemove)
        }
    }

    fun saveCurrentBankPreset() {
        val cleanAccount = withdrawAccountNumber.value.trim()
        val cleanAccountName = withdrawAccountName.value.trim().ifBlank { null }
        val bankName = withdrawBank.value.trim()
        val contact = withdrawContactNumber.value.trim().ifBlank { null }
        if (bankName.isBlank() || cleanAccount.length < 6) {
            withdrawError.value = if (selectedLanguage.value == "si") {
                "සුරැකීමට පෙර වලංගු බැංකු ගිණුම් අංකයක් ඇතුළත් කරන්න."
            } else {
                "Enter a valid bank account number (min 6 digits) before saving."
            }
            return
        }
        withdrawError.value = null
        saveBankPresetInternal(bankName, cleanAccount, cleanAccountName, contact)
    }

    private fun saveBankPresetInternal(bankName: String, accountNumber: String, accountName: String?, contact: String?) {
        viewModelScope.launch {
            isSavingPreset.value = true
            repository.saveBankPreset(
                userId = currentUserId,
                defaultName = profileName.value.ifBlank { currentUserName ?: "Player" },
                defaultPlayerId = profilePlayerId.value.ifBlank { withdrawPlayerId.value.ifBlank { depositPlayerId.value } },
                bank = bankName,
                accountNumber = accountNumber,
                accountName = accountName,
                contactNumber = contact
            )
            isSavingPreset.value = false
        }
    }

    fun removeBankPreset(presetId: String) {
        viewModelScope.launch {
            repository.removeBankPreset(currentUserId, presetId)
        }
    }

    fun applyBankPreset(preset: SavedBankPreset) {
        withdrawBank.value = preset.bank
        withdrawAccountNumber.value = preset.accountNumber
        preset.accountName?.let { withdrawAccountName.value = it }
        preset.contactNumber?.let { withdrawContactNumber.value = it }
        withdrawError.value = null
    }

    fun resetWithdrawalSuccess() {
        withdrawSuccessRef.value = null
        withdrawWhatsappMessage.value = null
    }

    fun trackTransaction(ref: String) {
        val clean = ref.trim()
        if (clean.isBlank()) return
        trackSearchQuery.value = clean
        searchJob?.cancel()
        trackedResult.value = null
        isTrackSearched.value = false
        trackingMessage.value = if (selectedLanguage.value == "si") "සොයමින්…" else "Searching…"

        searchJob = viewModelScope.launch {
            val result = repository.findTransactionByReference(currentUserId, isAdmin.value, clean)
            result.fold(
                onSuccess = { tx ->
                    trackedResult.value = tx
                    isTrackSearched.value = true
                    trackingMessage.value = null
                },
                onFailure = { e ->
                    trackingMessage.value = e.localizedMessage ?: "Unable to fetch transaction status."
                }
            )
        }
    }

    fun saveProfile() {
        if (profileName.value.isBlank() || profilePlayerId.value.trim().length < 4) return
        isSavingProfile.value = true
        profileMessage.value = null
        viewModelScope.launch {
            val result = repository.saveUserProfile(
                userId = currentUserId,
                name = profileName.value.trim(),
                playerId = profilePlayerId.value.trim()
            )
            result.fold(
                onSuccess = {
                    profileMessage.value = "Profile saved!"
                    if (depositPlayerId.value.isBlank()) depositPlayerId.value = profilePlayerId.value.trim()
                    if (withdrawPlayerId.value.isBlank()) withdrawPlayerId.value = profilePlayerId.value.trim()
                },
                onFailure = { e ->
                    profileMessage.value = e.localizedMessage ?: "Failed to save profile"
                }
            )
            isSavingProfile.value = false
        }
    }

    fun openAdminReview(tx: FirestoreTransaction) {
        selectedAdminTx.value = tx
        adminRejectionReason.value = tx.rejectionReason ?: ""
        adminAgentNote.value = tx.agentNote ?: if (tx.type == "DEPOSIT") {
            "MobCash මගින් 1xBet ගිණුමට (ID: ${tx.playerId}) මුදල් එක් කළා."
        } else {
            "ඔබගේ ${tx.bank ?: "බැංකු"} ගිණුමට මුදල් බැර කළා."
        }
        adminActionError.value = null
    }

    fun updateTransactionStatus(
        tx: FirestoreTransaction,
        status: TransactionStatus,
        agentNote: String? = null,
        rejectionReason: String? = null
    ) {
        if (isUpdatingAdminTx.value) return
        isUpdatingAdminTx.value = true
        adminActionError.value = null
        viewModelScope.launch {
            val result = repository.updateTransactionAndNotifyPlayer(
                tx = tx,
                newStatus = status,
                agentNote = agentNote,
                rejectionReason = rejectionReason
            )
            result.fold(
                onSuccess = {
                    selectedAdminTx.value = null
                    adminRejectionReason.value = ""
                    adminAgentNote.value = ""
                },
                onFailure = { e ->
                    adminActionError.value = e.localizedMessage ?: "Failed to update transaction"
                }
            )
            isUpdatingAdminTx.value = false
        }
    }

    fun saveAdminSettings() {
        val min = adminMinAmount.value.toLongOrNull() ?: 1000L
        val max = adminMaxAmount.value.toLongOrNull() ?: 500000L
        adminActionError.value = null
        viewModelScope.launch {
            val result = repository.saveAppConfig(
                whatsappNumber = adminWhatsapp.value.trim(),
                minTransaction = min,
                maxTransaction = max,
                promoCode = adminPromoCode.value.trim(),
                agentAccounts = adminAgentAccounts.value
            )
            result.onFailure { e ->
                adminActionError.value = e.localizedMessage ?: "Failed to save settings"
            }
        }
    }

    fun saveAdminAgentAccount(account: AgentDestinationAccount) {
        val current = adminAgentAccounts.value
        val exists = current.any { it.id == account.id }
        val updated = if (exists) {
            current.map { if (it.id == account.id) account else it }
        } else {
            current + account
        }
        adminAgentAccounts.value = updated
        saveAdminSettings()
    }

    fun deleteAdminAgentAccount(accountId: String) {
        val current = adminAgentAccounts.value
        if (current.size <= 1) {
            adminActionError.value = "At least one bank or iPay account must remain."
            return
        }
        adminAgentAccounts.value = current.filterNot { it.id == accountId }
        saveAdminSettings()
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(currentUserId, id)
        }
    }

    fun markAllNotificationsAsRead() {
        val unreadIds = allNotifications.value.filter { !it.read }.map { it.id }
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(currentUserId, unreadIds)
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "FastCashVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
