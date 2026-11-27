package com.example.fastcash.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.fastcash.ui.components.TimelineView
import com.example.fastcash.ui.components.WhatsAppHelper
import com.example.fastcash.ui.components.transactionLabels
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import com.example.fastcash.ui.viewmodel.ScreenDestination

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
    val receiptRef by viewModel.depositReceiptRef.collectAsState()
    val isSubmitting by viewModel.depositIsSubmitting.collectAsState()
    val successRef by viewModel.depositSuccessRef.collectAsState()
    val whatsappMsg by viewModel.depositWhatsappMessage.collectAsState()
    val error by viewModel.depositError.collectAsState()
    val selectedAccount by viewModel.selectedAgentAccount.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val agentAccounts = viewModel.agentAccounts

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

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
                            "නියෝජිත WhatsApp වෙත විස්තර යවා MobCash තහවුරු කිරීම බලන්න."
                        } else {
                            "Send the pre-filled details to agent WhatsApp (${appConfig.whatsappNumber}). You will receive an in-app alert once MobCash credits your 1xBet account."
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
                            .testTag("deposit_whatsapp_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
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
                                Text(
                                    text = acc.accountNumber,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 15.sp,
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.selectAgentAccount(acc)
                                    clipboard.setText(AnnotatedString(acc.accountNumber))
                                    Toast.makeText(
                                        context,
                                        "Copied ${acc.bankName}: ${acc.accountNumber}",
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
                            Text(
                                text = "${selectedAccount.displayLabel} · ${selectedAccount.accountNumber}",
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

                    // 1xBet Player ID
                    Column {
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

                    // Bank Receipt Reference
                    Column {
                        Text(
                            text = labels.getString(R.string.tx_receipt),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = receiptRef,
                            onValueChange = { viewModel.depositReceiptRef.value = it },
                            placeholder = { Text("e.g. PB-FT-889102 / Time & Ref", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deposit_receipt_ref_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
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
                        onClick = { viewModel.submitDeposit() },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("deposit_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
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
                                imageVector = Icons.Default.ArrowForward,
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
