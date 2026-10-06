package com.example.fastcash.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastcash.data.model.StatementProvider
import com.example.fastcash.data.model.TransactionEntity
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.data.model.VerificationStatus
import com.example.fastcash.ui.components.StatusPill
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: FastCashViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val searchQuery by viewModel.adminSearchQuery.collectAsState()
    val statusFilter by viewModel.adminStatusFilter.collectAsState()
    val selectedTx by viewModel.selectedAdminTx.collectAsState()
    val rejectionReason by viewModel.adminRejectionReason.collectAsState()

    val reconRows by viewModel.allReconRows.collectAsState()
    val reconSummary by viewModel.reconSummary.collectAsState()

    val whatsapp by viewModel.adminWhatsapp.collectAsState()
    val minAmt by viewModel.adminMinAmount.collectAsState()
    val maxAmt by viewModel.adminMaxAmount.collectAsState()
    val promo by viewModel.adminPromoCode.collectAsState()

    var selectedProvider by remember { mutableStateOf(StatementProvider.BOC) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Queue, 1: Recon, 2: Settings
    val context = LocalContext.current

    val filteredTransactions = allTransactions.filter { tx ->
        val matchesQuery = searchQuery.isBlank() ||
                tx.reference.contains(searchQuery, ignoreCase = true) ||
                tx.playerId.contains(searchQuery, ignoreCase = true)
        val matchesStatus = statusFilter == "ALL" || tx.status.name == statusFilter
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
                            text = "OPS // ADMIN DESK",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Control room",
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
                            text = "● LIVE QUEUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = DarkCard,
                    contentColor = NeonCyan
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("REQUESTS (${filteredTransactions.size})", style = MaterialTheme.typography.labelSmall) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("RECON", style = MaterialTheme.typography.labelSmall) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("SETTINGS", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        when (activeTab) {
            0 -> {
                // Request Queue Tab
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Search bar
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

                        // Status filter chips
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
                                        .border(0.8.dp, if (isSelected) NeonCyan else Color(0x33FFFFFF), RoundedCornerShape(4.dp))
                                        .clickable { viewModel.adminStatusFilter.value = st }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
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

                items(filteredTransactions) { tx ->
                    AdminRequestCard(
                        transaction = tx,
                        onReview = {
                            viewModel.selectedAdminTx.value = tx
                            viewModel.adminRejectionReason.value = tx.rejectionReason ?: ""
                        }
                    )
                }
            }

            1 -> {
                // Bank Reconciliation Tab
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "RECON // STATEMENT IMPORT",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Match bank activity",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select provider and import statement records to auto-verify customer deposits against pending references.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatementProvider.values().forEach { provider ->
                                    val isSelected = selectedProvider == provider
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) NeonCyan else DarkSurface)
                                            .border(0.8.dp, if (isSelected) NeonCyan else Color(0x33FFFFFF), RoundedCornerShape(4.dp))
                                            .clickable { selectedProvider = provider }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = provider.name.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) DarkBackground else TextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    viewModel.simulateBankReconciliation(selectedProvider)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("admin_recon_import_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = DarkBackground)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("IMPORT & VERIFY ${selectedProvider.name}", color = DarkBackground, style = MaterialTheme.typography.labelLarge)
                            }

                            if (reconSummary != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = reconSummary ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusGreen
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "RECONCILIATION QUEUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }

                items(reconRows) { row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#${row.externalId}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = row.description ?: "Bank transaction",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "LKR %,d · ${row.direction}".format(row.amount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (row.verificationStatus == VerificationStatus.VERIFIED)
                                                StatusGreen.copy(alpha = 0.2f) else DarkSurface
                                        )
                                        .border(
                                            0.8.dp,
                                            if (row.verificationStatus == VerificationStatus.VERIFIED)
                                                StatusGreen else Color(0x33FFFFFF),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = row.verificationStatus.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (row.verificationStatus == VerificationStatus.VERIFIED)
                                            StatusGreen else TextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                                if (row.verificationStatus != VerificationStatus.VERIFIED) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = {
                                            viewModel.resolveReconRow(row, VerificationStatus.EXCEPTION)
                                        }
                                    ) {
                                        Text("Ignore", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Settings Tab
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
                                text = "Network settings",
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
                                    Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
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

    // Review Modal / Card
    if (selectedTx != null) {
        val tx = selectedTx!!
        AlertDialog(
            onDismissRequest = { viewModel.selectedAdminTx.value = null },
            title = {
                Text("DECISION NOTE // ${tx.reference}", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${tx.type} · LKR %,d · Player: ${tx.playerId}".format(tx.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { viewModel.adminRejectionReason.value = it },
                        placeholder = { Text("Rejection reason (Required if rejecting)", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("admin_rejection_reason_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPink,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateTransactionStatus(tx.id, TransactionStatus.PROCESSING)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("PROCESS", color = DarkBackground, fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.updateTransactionStatus(tx.id, TransactionStatus.COMPLETED)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("COMPLETE", color = DarkBackground, fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                if (rejectionReason.isNotBlank()) {
                                    viewModel.updateTransactionStatus(tx.id, TransactionStatus.REJECTED, rejectionReason)
                                } else {
                                    Toast.makeText(context, "Rejection reason required!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("REJECT", color = TextPrimary, fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.selectedAdminTx.value = null }) {
                    Text("CLOSE", color = TextMuted)
                }
            },
            containerColor = DarkCard
        )
    }
}

@Composable
fun AdminRequestCard(
    transaction: TransactionEntity,
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
                Text(
                    text = "LKR %,d".format(transaction.amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusPill(status = transaction.status)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onReview,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Review", color = NeonCyan, fontSize = 10.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
