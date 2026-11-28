package com.example.fastcash.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastcash.R
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.ui.components.SavedPlayerIdChipsRow
import com.example.fastcash.ui.components.SlipImageHelper
import com.example.fastcash.ui.components.SlipOcrHelper
import com.example.fastcash.ui.components.TimelineView
import com.example.fastcash.ui.components.WhatsAppHelper
import com.example.fastcash.ui.components.transactionLabels
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import com.example.fastcash.ui.viewmodel.ScreenDestination
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(
    viewModel: FastCashViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.selectedLanguage.collectAsState()
    val labels = transactionLabels(language)
    val amount by viewModel.depositAmount.collectAsState()
    val playerId by viewModel.depositPlayerId.collectAsState()
    val paymentMethod by viewModel.depositPaymentMethod.collectAsState()
    val isSubmitting by viewModel.depositIsSubmitting.collectAsState()
    val successRef by viewModel.depositSuccessRef.collectAsState()
    val whatsappMsg by viewModel.depositWhatsappMessage.collectAsState()
    val submittedSlipUri by viewModel.depositSubmittedSlipUri.collectAsState()
    val slipBase64 by viewModel.depositSlipBase64.collectAsState()
    val slipSizeKb by viewModel.depositSlipSizeKb.collectAsState()
    val ocrSummary by viewModel.depositOcrSummary.collectAsState()
    val ocrMatched by viewModel.depositOcrMatched.collectAsState()
    val isProcessingSlip by viewModel.depositIsProcessingSlip.collectAsState()
    val error by viewModel.depositError.collectAsState()
    val selectedAccount by viewModel.selectedAgentAccount.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val savedPlayerIds by viewModel.savedPlayerIds.collectAsState()
    val isSavingPreset by viewModel.isSavingPreset.collectAsState()
    val agentAccounts by viewModel.agentAccounts.collectAsState()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var lastAutoSharedRef by remember { mutableStateOf<String?>(null) }
    var showResendConfirmation by remember { mutableStateOf(false) }
    var revealedAccountIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(successRef, submittedSlipUri) {
        val ref = successRef
        val slipUri = submittedSlipUri
        if (ref != null && ref != lastAutoSharedRef) {
            lastAutoSharedRef = ref
            if (slipUri != null && WhatsAppHelper.isWhatsAppInstalled(context)) {
                WhatsAppHelper.shareImageToWhatsApp(
                    context = context,
                    imageUri = slipUri,
                    message = whatsappMsg,
                    phoneNumber = appConfig.whatsappNumber
                )
            }
        }
    }

    val slipPreviewBitmap = remember(slipBase64) {
        SlipImageHelper.decodeBase64ToImageBitmap(slipBase64)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.depositIsProcessingSlip.value = true
            viewModel.depositError.value = null
            scope.launch {
                SlipImageHelper.processPickedSlip(context, uri).fold(
                    onSuccess = { processed ->
                        val ocrResult = SlipOcrHelper.extractDepositDetails(
                            jpegBytes = processed.jpegBytes,
                            agentAccounts = agentAccounts,
                            minAmount = appConfig.minTransaction,
                            maxAmount = appConfig.maxTransaction
                        )
                        viewModel.setProcessedSlip(processed, ocrResult)
                        Toast.makeText(
                            context,
                            if (ocrResult.hasAnyMatch) "Slip scanned & auto-filled (${processed.sizeKb} KB)"
                            else "Slip attached (${processed.sizeKb} KB)",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { err ->
                        viewModel.depositError.value = err.localizedMessage ?: "Failed to attach slip image"
                    }
                )
                viewModel.depositIsProcessingSlip.value = false
            }
        }
    }

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
                    .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
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
                        text = labels.getString(R.string.tx_accepted),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = labels.getString(R.string.tx_saved),
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
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (language == "si") {
                            "WhatsApp බෙදාගැනීම විවෘත කර ඇත. WhatsApp තුළ Send කිරීම තහවුරු කර, ගනුදෙනුවේ තත්ත්වය බලන්න."
                        } else {
                            "WhatsApp share opened. Confirm Send in WhatsApp, then track the request. Sending cannot be confirmed by this app."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    TimelineView(status = TransactionStatus.PENDING)

                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(
                        onClick = { showResendConfirmation = true },
                        modifier = Modifier.fillMaxWidth().testTag("deposit_whatsapp_btn")
                    ) {
                        Text(
                            if (language == "si") "අවශ්‍ය නම් නැවත WhatsApp වෙත යොමු කරන්න" else "Resend via WhatsApp (optional)",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val ref = successRef
                            viewModel.resetDepositSuccess()
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
                            .testTag("deposit_done_btn"),
                        border = BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = labels.getString(R.string.tx_track_action),
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        if (showResendConfirmation) {
            AlertDialog(
                onDismissRequest = { showResendConfirmation = false },
                title = { Text("Resend deposit details?") },
                text = { Text("This opens WhatsApp again. Check that the previous message was not already sent before resending.") },
                confirmButton = {
                    TextButton(onClick = {
                        showResendConfirmation = false
                        val slipUri = submittedSlipUri
                        if (slipUri != null) {
                            WhatsAppHelper.shareImageToWhatsApp(context, slipUri, whatsappMsg, appConfig.whatsappNumber)
                        } else {
                            WhatsAppHelper.openWhatsApp(context, appConfig.whatsappNumber, whatsappMsg)
                        }
                    }) { Text("RESEND") }
                },
                dismissButton = { TextButton(onClick = { showResendConfirmation = false }) { Text("CANCEL") } }
            )
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
                    text = labels.getString(R.string.tx_deposit_channel),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = labels.getString(R.string.tx_deposit_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (language == "si") {
                        "පහත ගිණුමක් copy කර මුදල් තැන්පත් කර, විස්තර පුරවා WhatsApp වෙත යවන්න."
                    } else {
                        "1. Copy a bank or iPay number below & transfer funds.\n2. Select that account and submit your details to send directly to agent WhatsApp."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Agent Bank & iPay Accounts Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = labels.getString(R.string.tx_destinations),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPink
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    agentAccounts.forEach { acc ->
                        val isSelected = selectedAccount.id == acc.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.10f) else DarkSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color(0x22FFFFFF),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.selectAgentAccount(acc) }
                                .padding(12.dp)
                                .testTag("agent_account_${acc.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = acc.bankName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                if (acc.method == "iPay wallet") NeonPink.copy(alpha = 0.2f)
                                                else NeonCyan.copy(alpha = 0.16f)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (acc.method == "iPay wallet") "iPay" else "BANK",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = if (acc.method == "iPay wallet") NeonPink else NeonCyan
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${acc.branch} · Name: ${acc.accountHolder}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val isRevealed = acc.id in revealedAccountIds
                                Text(
                                    text = if (isRevealed) acc.accountNumber else maskAccountNumber(acc.accountNumber),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 15.sp,
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isRevealed = acc.id in revealedAccountIds
                                IconButton(
                                    onClick = {
                                        revealedAccountIds = if (isRevealed) {
                                            revealedAccountIds - acc.id
                                        } else {
                                            revealedAccountIds + acc.id
                                        }
                                    },
                                    modifier = Modifier.testTag("toggle_mask_${acc.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isRevealed) "Hide account number" else "Show account number",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.selectAgentAccount(acc)
                                        clipboard.setText(AnnotatedString(acc.accountNumber))
                                        Toast.makeText(
                                            context,
                                            "Copied ${acc.bankName} (${maskAccountNumber(acc.accountNumber)})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.testTag("copy_account_${acc.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy ${acc.bankName} account number",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Deposit Form Card
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
                    // Selected Destination Badge
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "DEPOSITED TO",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontSize = 10.sp
                            )
                            val displayAccNum = if (selectedAccount.id in revealedAccountIds) {
                                selectedAccount.accountNumber
                            } else {
                                maskAccountNumber(selectedAccount.accountNumber)
                            }
                            Text(
                                text = "${selectedAccount.displayLabel} · $displayAccNum",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Amount (LKR)
                    Column {
                        Text(
                            text = labels.getString(R.string.tx_amount),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { viewModel.depositAmount.value = it },
                            placeholder = { Text("25000", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deposit_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Amount Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("5000", "10000", "25000", "50000").forEach { quickAmt ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(0.8.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .clickable { viewModel.depositAmount.value = quickAmt }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${quickAmt.toInt() / 1000}K",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }

                    // 1xBet Player ID (with 1-tap Saved Chips)
                    Column {
                        SavedPlayerIdChipsRow(
                            savedPlayerIds = savedPlayerIds,
                            currentPlayerId = playerId,
                            language = language,
                            accentColor = NeonCyan,
                            isSaving = isSavingPreset,
                            onSelect = { viewModel.depositPlayerId.value = it },
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
                            onValueChange = { viewModel.depositPlayerId.value = it },
                            placeholder = { Text("e.g. 184920412", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deposit_player_id_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Payment Method Selection
                    Column {
                        Text(
                            text = labels.getString(R.string.tx_method),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Bank transfer", "iPay wallet", "Cash deposit").forEach { method ->
                                val isSelected = paymentMethod == method
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else DarkSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NeonCyan else Color(0x33FFFFFF),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { viewModel.depositPaymentMethod.value = method }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (language == "si") {
                                            mapOf(
                                                "Bank transfer" to "බැංකු මාරුව",
                                                "Cash deposit" to "මුදල් තැන්පතුව",
                                                "iPay wallet" to "iPay පසුම්බිය"
                                            )[method] ?: method
                                        } else method,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = if (isSelected) NeonCyan else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Mandatory Bank Slip Photo Upload + On-Device OCR Auto-Fill
                    Column {
                        Text(
                            text = if (language == "si") {
                                "බැංකු ස්ලිප් ඡායාරූපය (අනිවාර්යයි) *"
                            } else {
                                "DEPOSIT SLIP / SCREENSHOT UPLOAD *"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (slipPreviewBitmap != null) StatusGreen.copy(alpha = 0.6f)
                                    else NeonPink.copy(alpha = 0.45f),
                                    RoundedCornerShape(6.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (slipPreviewBitmap != null) {
                                    Image(
                                        bitmap = slipPreviewBitmap,
                                        contentDescription = "Uploaded deposit slip preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    )

                                    if (!ocrSummary.isNullOrBlank()) {
                                        val pillColor = if (ocrMatched) StatusGreen else StatusYellow
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(pillColor.copy(alpha = 0.12f))
                                                .border(1.dp, pillColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                                .testTag("deposit_ocr_status_pill"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (ocrMatched) Icons.Default.AutoAwesome else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = pillColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = ocrSummary ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = pillColor,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = StatusGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (language == "si") {
                                                    "ස්ලිප් එක සූදානම් (${slipSizeKb ?: 0} KB)"
                                                } else {
                                                    "Slip attached (${slipSizeKb ?: 0} KB)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StatusGreen
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(
                                                onClick = {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Text("CHANGE", color = NeonCyan, fontSize = 11.sp)
                                            }
                                            TextButton(
                                                onClick = { viewModel.clearSelectedSlip() },
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Text("REMOVE", color = StatusRed, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = if (language == "si") {
                                            "ඔබ මුදල් දැමූ බැංකු ස්ලිප් එක හෝ iPay screenshot එක upload කරන්න. මුදල සහ ගිණුම ස්වයංක්‍රීයව පිරවෙනු ඇත."
                                        } else {
                                            "Required: upload your bank transfer slip or iPay screenshot. Amount and bank details auto-fill automatically."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = !isProcessingSlip && !isSubmitting,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("deposit_upload_slip_btn"),
                                        border = BorderStroke(1.dp, NeonCyan),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        if (isProcessingSlip) {
                                            CircularProgressIndicator(
                                                color = NeonCyan,
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (language == "si") "ස්ලිප් එක පරීක්ෂා කරමින්…" else "SCANNING SLIP DETAILS…",
                                                color = NeonCyan,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.UploadFile,
                                                contentDescription = "Upload Slip Photo",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (language == "si") "ස්ලිප් ෆොටෝ එක UPLOAD කරන්න" else "UPLOAD SLIP PHOTO",
                                                color = NeonCyan,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (error != null) {
                        Text(
                            text = error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusRed,
                            fontSize = 12.sp
                        )
                    } else if (slipBase64 == null) {
                        Text(
                            text = if (language == "si") {
                                "ඉල්ලීම යැවීමට පෙර බැංකු ස්ලිප් ෆොටෝ එක අනිවාර්යයෙන් upload කරන්න."
                            } else {
                                "Attach your deposit slip screenshot above to enable submission."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    val canSubmitDeposit = !isSubmitting && !isProcessingSlip &&
                        slipBase64 != null &&
                        (amount.trim().toLongOrNull() ?: 0L) in appConfig.minTransaction..appConfig.maxTransaction &&
                        playerId.trim().length >= 4

                    // Submit Button
                    Button(
                        onClick = { viewModel.submitDeposit() },
                        enabled = canSubmitDeposit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("deposit_submit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            disabledContainerColor = NeonCyan.copy(alpha = 0.25f),
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = DarkBackground,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                labels.getString(R.string.tx_sending),
                                color = DarkBackground,
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            Text(
                                text = if (language == "si") "ඉල්ලීම යවා WHATSAPP වෙත යොමු කරන්න" else "SUBMIT & SEND TO WHATSAPP",
                                color = DarkBackground,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun maskAccountNumber(accountNumber: String): String {
    val clean = accountNumber.trim()
    if (clean.length <= 4) return "••••"
    val maskLen = (clean.length - 4).coerceIn(4, 10)
    return "•".repeat(maskLen) + clean.takeLast(4)
}
