package com.example.fastcash.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastcash.data.model.FirestoreTransaction
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.ui.components.SlipImageHelper
import com.example.fastcash.ui.components.StatusPill
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: FastCashViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.adminFirestoreTransactions.collectAsState()
    val searchQuery by viewModel.adminSearchQuery.collectAsState()
    val statusFilter by viewModel.adminStatusFilter.collectAsState()
    val selectedTx by viewModel.selectedAdminTx.collectAsState()
    val agentNote by viewModel.adminAgentNote.collectAsState()
    val rejectionReason by viewModel.adminRejectionReason.collectAsState()
    val adminActionError by viewModel.adminActionError.collectAsState()
    val isUpdating by viewModel.isUpdatingAdminTx.collectAsState()

    val whatsapp by viewModel.adminWhatsapp.collectAsState()
    val minAmt by viewModel.adminMinAmount.collectAsState()
    val maxAmt by viewModel.adminMaxAmount.collectAsState()
    val promo by viewModel.adminPromoCode.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Queue, 1: Settings
    val context = LocalContext.current

    val filteredTransactions = allTransactions.filter { tx ->
        val matchesQuery = searchQuery.isBlank() ||
            tx.reference.contains(searchQuery, ignoreCase = true) ||
            tx.playerId.contains(searchQuery, ignoreCase = true)
        val matchesStatus = statusFilter == "ALL" || tx.status == statusFilter
        matchesQuery && matchesStatus
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OPS // AGENT CONTROL ROOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MobCash & Payout Desk",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, StatusGreen, RoundedCornerShape(4.dp))
                            .background(StatusGreen.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "● LIVE SYNC",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = DarkCard,
                    contentColor = NeonCyan
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                "REQUESTS (${filteredTransactions.size})",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text("SETTINGS", style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            }
        }

        when (activeTab) {
            0 -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.adminSearchQuery.value = it },
                            placeholder = { Text("Search reference or Player ID", color = TextMuted) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ALL", "PENDING", "PROCESSING", "COMPLETED", "REJECTED").forEach { st ->
                                val isSelected = statusFilter == st
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) NeonCyan else DarkSurface)
                                        .border(
                                            0.8.dp,
                                            if (isSelected) NeonCyan else Color(0x33FFFFFF),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { viewModel.adminStatusFilter.value = st }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("admin_filter_$st")
                                ) {
                                    Text(
                                        text = st,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) DarkBackground else TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Text(
                                text = "No matching customer requests in this queue.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(filteredTransactions) { tx ->
                        AdminRequestCard(
                            transaction = tx,
                            onReview = { viewModel.openAdminReview(tx) }
                        )
                    }
                }
            }

            1 -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonPink.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "CONFIG // NETWORK SERVICE CONTROL",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonPink
                            )
                            Text(
                                text = "Agent WhatsApp & Limits",
                                style = MaterialTheme.typography.titleLarge
                            )

                            OutlinedTextField(
                                value = whatsapp,
                                onValueChange = { viewModel.adminWhatsapp.value = it },
                                label = { Text("WhatsApp Support Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )

                            OutlinedTextField(
                                value = minAmt,
                                onValueChange = { viewModel.adminMinAmount.value = it },
                                label = { Text("Minimum Transaction (LKR)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )

                            OutlinedTextField(
                                value = maxAmt,
                                onValueChange = { viewModel.adminMaxAmount.value = it },
                                label = { Text("Maximum Transaction (LKR)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )

                            OutlinedTextField(
                                value = promo,
                                onValueChange = { viewModel.adminPromoCode.value = it },
                                label = { Text("Agent Promo Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )

                            Button(
                                onClick = {
                                    viewModel.saveAdminSettings()
                                    Toast.makeText(context, "Settings updated!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_save_settings_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("SAVE SETTINGS", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    // Decision & Player Notification Dialog
    if (selectedTx != null) {
        val tx = selectedTx!!
        AlertDialog(
            onDismissRequest = { if (!isUpdating) viewModel.selectedAdminTx.value = null },
            title = {
                Text(
                    "NOTIFY PLAYER // ${tx.reference}",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan
                )
            },
            text = {
                val slipBitmap = remember(tx.id, tx.slipImageBase64) {
                    SlipImageHelper.decodeBase64ToImageBitmap(tx.slipImageBase64)
                }
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${tx.type} · LKR %,d · Player ID: ${tx.playerId}".format(tx.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    if (!tx.agentAccountLabel.isNullOrBlank()) {
                        Text(
                            text = "Deposited To: ${tx.agentAccountLabel} (${tx.agentAccountNumber ?: ""})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonCyan,
                            fontSize = 12.sp
                        )
                    }
                    if (!tx.receiptKey.isNullOrBlank()) {
                        Text(
                            text = "Receipt Ref: ${tx.receiptKey}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (slipBitmap != null) {
                        Text(
                            text = "ATTACHED DEPOSIT SLIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreen
                        )
                        Image(
                            bitmap = slipBitmap,
                            contentDescription = "Customer uploaded deposit slip",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .border(1.dp, StatusGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        )
                    }
                    if (!tx.securityCode.isNullOrBlank()) {
                        Text(
                            text = "1xBet Code: ${tx.securityCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonPink,
                            fontSize = 12.sp
                        )
                    }
                    if (!tx.bank.isNullOrBlank()) {
                        Text(
                            text = "Payout Bank: ${tx.bank} · Acc: ${tx.accountNumber ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedTextField(
                        value = agentNote,
                        onValueChange = { viewModel.adminAgentNote.value = it },
                        label = { Text("Confirmation message sent to player") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_agent_note_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { viewModel.adminRejectionReason.value = it },
                        placeholder = { Text("Rejection reason (Required if rejecting)", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_rejection_reason_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPink,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    if (adminActionError != null) {
                        Text(
                            text = adminActionError ?: "",
                            color = StatusRed,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateTransactionStatus(
                                    tx = tx,
                                    status = TransactionStatus.PROCESSING,
                                    agentNote = null
                                )
                            },
                            enabled = !isUpdating,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("PROCESS", color = DarkBackground, fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.updateTransactionStatus(
                                    tx = tx,
                                    status = TransactionStatus.COMPLETED,
                                    agentNote = agentNote
                                )
                            },
                            enabled = !isUpdating,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("COMPLETE", color = DarkBackground, fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                if (rejectionReason.isNotBlank()) {
                                    viewModel.updateTransactionStatus(
                                        tx = tx,
                                        status = TransactionStatus.REJECTED,
                                        rejectionReason = rejectionReason
                                    )
                                } else {
                                    Toast.makeText(context, "Rejection reason required!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isUpdating,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("REJECT", color = TextPrimary, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { viewModel.selectedAdminTx.value = null },
                    enabled = !isUpdating
                ) {
                    Text("CLOSE", color = TextMuted)
                }
            },
            containerColor = DarkCard
        )
    }
}

@Composable
fun AdminRequestCard(
    transaction: FirestoreTransaction,
    onReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.reference,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${transaction.type} · Player: ${transaction.playerId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 11.sp
                )
                if (!transaction.agentAccountLabel.isNullOrBlank()) {
                    Text(
                        text = transaction.agentAccountLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                } else if (!transaction.bank.isNullOrBlank()) {
                    Text(
                        text = "${transaction.bank} · ${transaction.accountNumber ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "LKR %,d".format(transaction.amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                if (!transaction.slipImageBase64.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SLIP PHOTO ATTACHED",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusGreen,
                        fontSize = 9.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(status = transaction.parsedStatus)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onReview,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Notify / Review", color = NeonCyan, fontSize = 10.sp)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
