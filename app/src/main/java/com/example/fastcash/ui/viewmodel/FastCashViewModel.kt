package com.example.fastcash.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fastcash.data.local.AppDatabase
import com.example.fastcash.data.model.*
import com.example.fastcash.data.repository.FastCashRepository
import kotlinx.coroutines.flow.*
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

class FastCashViewModel(application: Application, customer: SignedInCustomer) : AndroidViewModel(application) {
    private val db = AppDatabase.open(application, customer.scope)
    private val repository = FastCashRepository(
        db, customer.api, application.getSharedPreferences("requests_${customer.scope}", 0)
    )

    override fun onCleared() {
        db.close()
        super.onCleared()
    }

    val syncError = MutableStateFlow<String?>(null)

    // Navigation & Global UI State
    val currentScreen = MutableStateFlow(ScreenDestination.HOME)
    val selectedLanguage = MutableStateFlow("en")
    val isNotificationsSheetOpen = MutableStateFlow(false)
    val isAdminMode = MutableStateFlow(false)

    // Data streams
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = allNotifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeAgentAccounts: StateFlow<List<AgentAccountEntity>> = repository.activeAgentAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReconRows: StateFlow<List<ReconRowEntity>> = repository.allReconRows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Language Content
    val content: StateFlow<LocalizedContent> = selectedLanguage.map { lang ->
        when (lang) {
            "si" -> LocalizedContent(
                heroKicker = "නිල නියෝජිත මෙහෙයුම් // ශ්‍රී ලංකාව",
                heroTitle = "විශ්වාසය වගේ වේගයෙන් මුදල් ගමන් කරයි.",
                heroText = "ඔබගේ 1xBet wallet එක සහ Fast Cash නියෝජිතයන් අතර පැහැදිලි, track කළ හැකි මාර්ගයක්. එක් වරක් submit කරන්න. සෑම status එකක්ම බලන්න.",
                deposit = "Deposit ආරම්භ කරන්න",
                withdraw = "Withdrawal ඉල්ලන්න",
                track = "Request track කරන්න",
                live = "නියෝජිතයන් online",
                secure = "ආරක්ෂාව මුලින්",
                steps = listOf("මාර්ගය තෝරන්න", "විස්තර යවන්න", "Timeline බලන්න"),
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
                steps = listOf("வழியை தேர்ந்தெடு", "விவரங்களை அனுப்பு", "Timeline பார்"),
                support = "மனித உதவி வேண்டுமா? சேவை நேரத்தில் WhatsApp உதவி உள்ளது.",
                responsible = "18+ மட்டும். சூதாட்டம் பொழுதுபோக்காக இருக்க வேண்டும். கடன் பணத்தை பயன்படுத்த வேண்டாம்."
            )
            else -> LocalizedContent(
                heroKicker = "OFFICIAL AGENT OPERATIONS // SRI LANKA",
                heroTitle = "Your money moves at the speed of trust.",
                heroText = "A clear, trackable bridge between your 1xBet wallet and Fast Cash agents. Submit once. See every status. Stay in control.",
                deposit = "Start a deposit",
                withdraw = "Request withdrawal",
                track = "Track request",
                live = "AGENTS ONLINE",
                secure = "Protected by design",
                steps = listOf("Choose a route", "Send the details", "Watch the timeline"),
                support = "Need a human? WhatsApp support is available during service hours.",
                responsible = "18+ only. Gambling should be entertainment. Never use borrowed money, and stop if it stops being fun."
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LocalizedContent(
            heroKicker = "OFFICIAL AGENT OPERATIONS // SRI LANKA",
            heroTitle = "Your money moves at the speed of trust.",
            heroText = "A clear, trackable bridge between your 1xBet wallet and Fast Cash agents. Submit once. See every status. Stay in control.",
            deposit = "Start a deposit",
            withdraw = "Request withdrawal",
            track = "Track request",
            live = "AGENTS ONLINE",
            secure = "Protected by design",
            steps = listOf("Choose a route", "Send the details", "Watch the timeline"),
            support = "Need a human? WhatsApp support is available during service hours.",
            responsible = "18+ only. Gambling should be entertainment. Never use borrowed money, and stop if it stops being fun."
        )
    )

    // Deposit State
    val depositAmount = MutableStateFlow("")
    val depositPlayerId = MutableStateFlow("")
    val depositPaymentMethod = MutableStateFlow("Bank transfer")
    val depositReceiptRef = MutableStateFlow("")
    val depositIsSubmitting = MutableStateFlow(false)
    val depositSuccessRef = MutableStateFlow<String?>(null)
    val depositError = MutableStateFlow<String?>(null)

    // Withdrawal State
    val withdrawAmount = MutableStateFlow("")
    val withdrawPlayerId = MutableStateFlow("")
    val withdrawSecurityCode = MutableStateFlow("")
    val withdrawBank = MutableStateFlow("Bank of Ceylon (BOC)")
    val withdrawAccountNumber = MutableStateFlow("")
    val withdrawContactNumber = MutableStateFlow("")
    val withdrawIsSubmitting = MutableStateFlow(false)
    val withdrawSuccessRef = MutableStateFlow<String?>(null)
    val withdrawError = MutableStateFlow<String?>(null)

    // Tracking State
    val trackSearchQuery = MutableStateFlow("")
    val trackedResult = MutableStateFlow<TransactionEntity?>(null)
    val isTrackSearched = MutableStateFlow(false)

    // Profile State
    val profileName = MutableStateFlow("")
    val profilePlayerId = MutableStateFlow("")
    val isSavingProfile = MutableStateFlow(false)

    // Admin State
    val adminSearchQuery = MutableStateFlow("")
    val adminStatusFilter = MutableStateFlow("ALL")
    val selectedAdminTx = MutableStateFlow<TransactionEntity?>(null)
    val adminRejectionReason = MutableStateFlow("")
    val adminWhatsapp = MutableStateFlow("+94765865387")
    val adminMinAmount = MutableStateFlow("1000")
    val adminMaxAmount = MutableStateFlow("500000")
    val adminPromoCode = MutableStateFlow("VGSL")
    val reconSummary = MutableStateFlow<String?>(null)

    init {
        refreshData()
        // Pre-fill profile if available
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
    }

    fun setLanguage(lang: String) {
        selectedLanguage.value = lang
    }

    fun navigateTo(screen: ScreenDestination) {
        currentScreen.value = screen
    }

    fun submitDeposit() {
        if (depositIsSubmitting.value) return
        val amount = depositAmount.value.toLongOrNull()
        if (amount == null || amount < 1000 || amount > 500000) {
            depositError.value = if (selectedLanguage.value == "si") "මුදල රු. 1,000 සහ 500,000 අතර විය යුතුයි." else "Amount must be between LKR 1,000 and 500,000"
            return
        }
        if (depositPlayerId.value.trim().length < 4) {
            depositError.value = if (selectedLanguage.value == "si") "වලංගු ක්‍රීඩක අංකයක් ඇතුළත් කරන්න." else "Please enter a valid 1xBet Player ID"
            return
        }

        depositIsSubmitting.value = true
        depositError.value = null
        viewModelScope.launch {
            try {
                val ref = repository.createDeposit(
                    amount = amount,
                    playerId = depositPlayerId.value.trim(),
                    paymentMethod = depositPaymentMethod.value,
                    receiptKey = depositReceiptRef.value.trim().ifBlank { null }
                )
                depositSuccessRef.value = ref
                // Reset form
                depositAmount.value = ""
                depositReceiptRef.value = ""
            } catch (e: Exception) {
                depositError.value = e.message ?: "Failed to submit deposit request"
            } finally {
                depositIsSubmitting.value = false
            }
        }
    }

    fun resetDepositSuccess() {
        depositSuccessRef.value?.let(repository::acknowledge)
        depositSuccessRef.value = null
    }

    fun submitWithdrawal() {
        if (withdrawIsSubmitting.value) return
        val amount = withdrawAmount.value.toLongOrNull()
        if (amount == null || amount < 1000 || amount > 500000) {
            withdrawError.value = if (selectedLanguage.value == "si") "මුදල රු. 1,000 සහ 500,000 අතර විය යුතුයි." else "Amount must be between LKR 1,000 and 500,000"
            return
        }
        if (withdrawPlayerId.value.trim().length < 4) {
            withdrawError.value = if (selectedLanguage.value == "si") "වලංගු ක්‍රීඩක අංකයක් ඇතුළත් කරන්න." else "Please enter a valid 1xBet Player ID"
            return
        }
        if (withdrawSecurityCode.value.trim().isBlank()) {
            withdrawError.value = "Please enter the 1xBet Cash withdrawal security code"
            return
        }
        if (withdrawAccountNumber.value.trim().length < 6) {
            withdrawError.value = "Please enter a valid bank account number"
            return
        }

        withdrawIsSubmitting.value = true
        withdrawError.value = null
        viewModelScope.launch {
            try {
                val ref = repository.createWithdrawal(
                    amount = amount,
                    playerId = withdrawPlayerId.value.trim(),
                    securityCode = withdrawSecurityCode.value.trim(),
                    bank = withdrawBank.value,
                    accountNumber = withdrawAccountNumber.value.trim(),
                    contactNumber = withdrawContactNumber.value.trim()
                )
                withdrawSuccessRef.value = ref
                // Reset form
                withdrawAmount.value = ""
                withdrawSecurityCode.value = ""
                withdrawAccountNumber.value = ""
                withdrawContactNumber.value = ""
            } catch (e: Exception) {
                withdrawError.value = e.message ?: "Failed to submit withdrawal request"
            } finally {
                withdrawIsSubmitting.value = false
            }
        }
    }

    fun resetWithdrawalSuccess() {
        withdrawSuccessRef.value?.let(repository::acknowledge)
        withdrawSuccessRef.value = null
    }

    val trackingMessage = MutableStateFlow<String?>(null)
    private var searchJob: kotlinx.coroutines.Job? = null

    fun trackTransaction(ref: String) {
        val clean = ref.trim()
        if (clean.isBlank()) return
        trackSearchQuery.value = clean
        searchJob?.cancel()
        trackedResult.value = null
        isTrackSearched.value = false
        trackingMessage.value = "සොයමින්…"
        searchJob = viewModelScope.launch {
            try {
                trackedResult.value = repository.findTransactionByReference(clean)
                isTrackSearched.value = true
                trackingMessage.value = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                trackingMessage.value = "තත්ත්වය ලබාගත නොහැක. නැවත උත්සාහ කරන්න."
            }
        }
    }

    fun saveProfile() {
        if (profileName.value.isBlank() || profilePlayerId.value.isBlank()) return
        isSavingProfile.value = true
        viewModelScope.launch {
            repository.saveUserProfile(profileName.value.trim(), profilePlayerId.value.trim())
            isSavingProfile.value = false
        }
    }

    fun updateTransactionStatus(id: Long, status: TransactionStatus, reason: String? = null) {
        viewModelScope.launch {
            repository.updateTransactionStatus(id, status, reason)
            selectedAdminTx.value = null
            adminRejectionReason.value = ""
        }
    }

    fun simulateBankReconciliation(provider: StatementProvider) {
        viewModelScope.launch {
            val pending = allTransactions.value.filter { it.status == TransactionStatus.PENDING }
            var verified = 0
            val newRow = ReconRowEntity(
                externalId = "${provider.name}-${System.currentTimeMillis() % 100000}",
                provider = provider,
                direction = "CREDIT",
                amount = 25000L,
                reference = pending.firstOrNull()?.reference ?: "CDM-DEPOSIT",
                description = "Statement row auto-imported from ${provider.name}",
                verificationStatus = if (pending.isNotEmpty()) VerificationStatus.VERIFIED else VerificationStatus.UNMATCHED,
                transactionId = pending.firstOrNull()?.id
            )
            repository.addReconRow(newRow)
            if (pending.isNotEmpty()) {
                val target = pending.first()
                repository.updateTransactionStatus(target.id, TransactionStatus.PROCESSING, null)
                verified = 1
            }
            reconSummary.value = "1 statement row processed · $verified verified · 0 ambiguous"
        }
    }

    fun resolveReconRow(row: ReconRowEntity, newStatus: VerificationStatus) {
        viewModelScope.launch {
            repository.updateReconRow(row.copy(verificationStatus = newStatus))
        }
    }

    fun saveAdminSettings() {
        val min = adminMinAmount.value.toLongOrNull() ?: 1000L
        val max = adminMaxAmount.value.toLongOrNull() ?: 500000L
        viewModelScope.launch {
            repository.saveAppSettings(
                AppSettingsEntity(
                    whatsappNumber = adminWhatsapp.value.trim(),
                    minTransaction = min,
                    maxTransaction = max,
                    promoCode = adminPromoCode.value.trim()
                )
            )
        }
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }


    fun refreshData() {
        viewModelScope.launch {
            try {
                repository.syncFromRemote()
                syncError.value = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                syncError.value = "දත්ත යාවත්කාලීන කිරීමට නොහැකි විය."
            }
        }
    }
}
