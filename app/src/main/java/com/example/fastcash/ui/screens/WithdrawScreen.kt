package com.example.fastcash.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastcash.R
import com.example.fastcash.data.model.SriLankaBanks
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.ui.components.SavedBankPresetsRow
import com.example.fastcash.ui.components.SavedPlayerIdChipsRow
import com.example.fastcash.ui.components.TimelineView
import com.example.fastcash.ui.components.WhatsAppHelper
import com.example.fastcash.ui.components.transactionLabels
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import com.example.fastcash.ui.viewmodel.ScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawScreen(
    viewModel: FastCashViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.selectedLanguage.collectAsState()
    val labels = transactionLabels(language)
    val amount by viewModel.withdrawAmount.collectAsState()
    val playerId by viewModel.withdrawPlayerId.collectAsState()
    val securityCode by viewModel.withdrawSecurityCode.collectAsState()
    val bank by viewModel.withdrawBank.collectAsState()
    val accountNumber by viewModel.withdrawAccountNumber.collectAsState()
    val accountName by viewModel.withdrawAccountName.collectAsState()
    val contactNumber by viewModel.withdrawContactNumber.collectAsState()
    val isSubmitting by viewModel.withdrawIsSubmitting.collectAsState()
    val successRef by viewModel.withdrawSuccessRef.collectAsState()
    val whatsappMsg by viewModel.withdrawWhatsappMessage.collectAsState()
    val error by viewModel.withdrawError.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val savedPlayerIds by viewModel.savedPlayerIds.collectAsState()
    val savedBankPresets by viewModel.savedBankPresets.collectAsState()
    val isSavingPreset by viewModel.isSavingPreset.collectAsState()

    val context = LocalContext.current
    var showBankPicker by remember { mutableStateOf(false) }
    var bankSearchQuery by remember { mutableStateOf("") }
    val bankList = SriLankaBanks.allBanks

    if (successRef != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .border(1.dp, NeonPink, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .border(1.5.dp, StatusGreen, RoundedCornerShape(8.dp))
                            .background(StatusGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = StatusGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = labels.getString(R.string.tx_queued),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPink,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = labels.getString(R.string.tx_withdraw_saved),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = labels.getString(R.string.tx_reference),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = successRef ?: "",
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (language == "si") {
                            "නියෝජිත WhatsApp වෙත විස්තර යවන්න. MobCash පරීක්ෂාවෙන් පසු ඔබට app එක හරහා දැනුම් දෙනු ලැබේ."
                        } else {
                            "Send your withdrawal details to agent WhatsApp (${appConfig.whatsappNumber}). You will receive an in-app alert when your bank payout is sent."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    TimelineView(status = TransactionStatus.PENDING)

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            WhatsAppHelper.openWhatsApp(
                                context = context,
                                phoneNumber = appConfig.whatsappNumber,
                                message = whatsappMsg
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("withdraw_whatsapp_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Send to WhatsApp",
                            tint = DarkBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "si") "WHATSAPP වෙත යවන්න" else "SEND TO AGENT WHATSAPP",
                            color = DarkBackground,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val ref = successRef
                            viewModel.resetWithdrawalSuccess()
                            if (ref != null) {
                                viewModel.trackTransaction(ref)
                                viewModel.navigateTo(ScreenDestination.TRACK)
                            } else {
                                viewModel.navigateTo(ScreenDestination.HOME)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("withdraw_done_btn"),
                        border = BorderStroke(1.dp, NeonPink),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = labels.getString(R.string.tx_track_action),
                            color = NeonPink,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column {
                Text(
                    text = labels.getString(R.string.tx_withdraw_channel),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonPink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = labels.getString(R.string.tx_withdraw_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labels.getString(R.string.tx_withdraw_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Amount
                    Column {
                        Text(
                            text = labels.getString(R.string.tx_amount),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { viewModel.withdrawAmount.value = it },
                            placeholder = { Text("12500", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // 1xBet Player ID (with 1-tap Saved Chips)
                    Column {
                        SavedPlayerIdChipsRow(
                            savedPlayerIds = savedPlayerIds,
                            currentPlayerId = playerId,
                            language = language,
                            accentColor = NeonPink,
                            isSaving = isSavingPreset,
                            onSelect = { viewModel.withdrawPlayerId.value = it },
                            onSaveCurrent = { viewModel.savePlayerIdPreset(playerId) },
                            onRemove = { viewModel.removePlayerIdPreset(it) }
                        )
                        if (savedPlayerIds.isNotEmpty() || playerId.trim().length >= 4) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Text(
                            text = labels.getString(R.string.tx_player),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = playerId,
                            onValueChange = { viewModel.withdrawPlayerId.value = it },
                            placeholder = { Text("e.g. 184920412", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_player_id_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Security Code
                    Column {
                        Text(
                            text = labels.getString(R.string.tx_code),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = securityCode,
                            onValueChange = { viewModel.withdrawSecurityCode.value = it },
                            placeholder = { Text("From 1xBet Cash withdrawal confirmation", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_security_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Saved Bank Presets (1-tap quick fill) + All Sri Lankan Banks Selector
                    Column {
                        SavedBankPresetsRow(
                            savedPresets = savedBankPresets,
                            currentBank = bank,
                            currentAccountNumber = accountNumber,
                            language = language,
                            isSaving = isSavingPreset,
                            onSelect = { viewModel.applyBankPreset(it) },
                            onSaveCurrent = { viewModel.saveCurrentBankPreset() },
                            onRemove = { viewModel.removeBankPreset(it) }
                        )
                        if (savedBankPresets.isNotEmpty() || accountNumber.trim().length >= 6) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Text(
                            text = if (language == "si") "ලංකාවේ බැංකුව තෝරන්න (ALL SRI LANKAN BANKS)" else labels.getString(R.string.tx_bank),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            onClick = { showBankPicker = true },
                            color = DarkSurface,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("withdraw_bank_picker_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bank,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Sri Lankan Bank",
                                    tint = NeonCyan
                                )
                            }
                        }
                    }

                    // Account Number
                    Column {
                        Text(
                            text = labels.getString(R.string.tx_account),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { viewModel.withdrawAccountNumber.value = it },
                            placeholder = { Text("Enter account number carefully", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_account_number_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Account Name
                    Column {
                        Text(
                            text = labels.getString(R.string.tx_account_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = accountName,
                            onValueChange = { viewModel.withdrawAccountName.value = it },
                            placeholder = { Text("Enter bank account holder name", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_account_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Contact Number
                    Column {
                        Text(
                            text = labels.getString(R.string.tx_phone),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = { viewModel.withdrawContactNumber.value = it },
                            placeholder = { Text("07X XXX XXXX", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_contact_number_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    if (error != null) {
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusRed,
                            fontSize = 12.sp
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = { viewModel.submitWithdrawal() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("withdraw_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = TextPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                labels.getString(R.string.tx_sending),
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            Text(
                                text = if (language == "si") "ඉල්ලීම යවා WHATSAPP වෙත යොමු කරන්න" else "SUBMIT & SEND TO WHATSAPP",
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBankPicker) {
        val filteredBanks = remember(bankSearchQuery, bankList) {
            val q = bankSearchQuery.trim()
            if (q.isBlank()) bankList else bankList.filter { it.contains(q, ignoreCase = true) }
        }
        ModalBottomSheet(
            onDismissRequest = {
                showBankPicker = false
                bankSearchQuery = ""
            },
            containerColor = DarkCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = if (language == "si") "ශ්‍රී ලංකාවේ බැංකු සහ මුදල් පසුම්බි (${bankList.size})" else "SRI LANKA BANKS & WALLETS (${bankList.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = bankSearchQuery,
                    onValueChange = { bankSearchQuery = it },
                    placeholder = { Text("Search bank (BOC, HNB, NDB, RDB, Sampath...)", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_bank_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredBanks) { item ->
                        val isSelected = item == bank
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonPink.copy(alpha = 0.16f) else DarkSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonPink else Color(0x22FFFFFF),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    viewModel.withdrawBank.value = item
                                    showBankPicker = false
                                    bankSearchQuery = ""
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) NeonPink else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = NeonPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
