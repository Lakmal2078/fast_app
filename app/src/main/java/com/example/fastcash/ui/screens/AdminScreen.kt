package com.example.fastcash.ui.screens

import android.widget.Toast
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import com.example.fastcash.data.model.AgentDestinationAccount
import com.example.fastcash.data.model.FirestoreTransaction
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.ui.components.SlipImageHelper
import com.example.fastcash.ui.components.StatusPill
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import java.util.UUID

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
    val adminAgentAccounts by viewModel.adminAgentAccounts.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Queue, 1: Settings
    var typeFilter by remember { mutableStateOf("ALL") }
    var dateFilter by remember { mutableStateOf("ALL") }
    var editingAccount by remember { mutableStateOf<AgentDestinationAccount?>(null) }
    var isAddingAccount by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filteredTransactions = allTransactions.filter { tx ->
        val matchesQuery = searchQuery.isBlank() ||
            tx.reference.contains(searchQuery, ignoreCase = true) ||
            tx.playerId.contains(searchQuery, ignoreCase = true)
        val matchesStatus = statusFilter == "ALL" || tx.status == statusFilter
        val matchesType = typeFilter == "ALL" || tx.type == typeFilter
        val now = System.currentTimeMillis()
        val age = now - tx.timestampMillis
        val matchesDate = when (dateFilter) {
            "TODAY" -> SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(tx.timestampMillis)) ==
                SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(now))
            "7 DAYS" -> age in 0L..(7L * 24 * 60 * 60 * 1000)
            "30 DAYS" -> age in 0L..(30L * 24 * 60 * 60 * 1000)
            else -> true
        }
        matchesQuery && matchesStatus && matchesType && matchesDate
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

                item {
                    AdminTransactionSummary(allTransactions, filteredTransactions)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL", "DEPOSIT", "WITHDRAWAL").forEach { kind ->
                            FilterChip(selected = typeFilter == kind, onClick = { typeFilter = kind }, label = { Text(kind) })
                        }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ALL", "TODAY", "7 DAYS", "30 DAYS").forEach { range ->
                            FilterChip(selected = dateFilter == range, onClick = { dateFilter = range }, label = { Text(range) })
                        }
                    }
                    OutlinedButton(onClick = {
                        if (viewModel.isAdmin.value) {
                            val csv = buildAdminCsv(filteredTransactions)
                            val reportDir = File(context.cacheDir, "reports").apply { mkdirs() }
                            val report = File(reportDir, "fastcash-transactions-${System.currentTimeMillis()}.csv")
                            report.writeText("\uFEFF" + csv, Charsets.UTF_8)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", report)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export filtered transaction report"))
                        }
                    }, modifier = Modifier.fillMaxWidth().testTag("admin_export_csv")) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("EXPORT FILTERED CSV (${filteredTransactions.size})")
                    }
                    Text("Only transactions loaded in the admin queue are exported. No security codes or slip images are included.", color = TextMuted, fontSize = 10.sp)
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

                            if (adminActionError != null) {
                                Text(
                                    text = adminActionError ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusRed,
                                    fontSize = 12.sp
                                )
                            }

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

                // Agent Bank & iPay Accounts Manager
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CONFIG // DEPOSIT DESTINATIONS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan
                                    )
                                    Text(
                                        text = "Agent Bank & iPay Accounts",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        isAddingAccount = true
                                        editingAccount = AgentDestinationAccount(
                                            id = "acc_" + UUID.randomUUID().toString().replace("-", "").take(8),
                                            bankName = "",
                                            branch = "",
                                            accountHolder = "",
                                            accountNumber = "",
                                            method = "Bank transfer"
                                        )
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = NeonCyan.copy(alpha = 0.16f),
                                        contentColor = NeonCyan
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.testTag("admin_add_bank_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ADD", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            adminAgentAccounts.forEach { acc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${acc.bankName} (${acc.method})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${acc.branch} · ${acc.accountHolder}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = acc.accountNumber,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontSize = 13.sp,
                                            color = NeonCyan
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                isAddingAccount = false
                                                editingAccount = acc
                                            },
                                            modifier = Modifier.testTag("admin_edit_bank_${acc.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit bank account",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        if (adminAgentAccounts.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteAdminAgentAccount(acc.id)
                                                    Toast.makeText(context, "Removed ${acc.bankName}", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("admin_delete_bank_${acc.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete bank account",
                                                    tint = StatusRed,
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
            }
        }
    }

    // Add / Edit Agent Bank Account Dialog
    if (editingAccount != null) {
        val target = editingAccount!!
        var bankName by remember(target.id) { mutableStateOf(target.bankName) }
        var branch by remember(target.id) { mutableStateOf(target.branch) }
        var holder by remember(target.id) { mutableStateOf(target.accountHolder) }
        var accNumber by remember(target.id) { mutableStateOf(target.accountNumber) }
        var method by remember(target.id) { mutableStateOf(target.method.ifBlank { "Bank transfer" }) }
        var formError by remember(target.id) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingAccount = null },
            containerColor = DarkCard,
            title = {
                Text(
                    text = if (isAddingAccount) "Add Bank / iPay Account" else "Edit Bank / iPay Account",
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Bank transfer", "iPay wallet").forEach { option ->
                            FilterChip(
                                selected = method == option,
                                onClick = { method = option },
                                label = { Text(option, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank / Wallet Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        label = { Text("Branch / Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = holder,
                        onValueChange = { holder = it },
                        label = { Text("Account Holder Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = accNumber,
                        onValueChange = { accNumber = it },
                        label = { Text("Account / iPay Number *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (formError != null) {
                        Text(formError!!, color = StatusRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bankName.isBlank() || holder.isBlank() || accNumber.isBlank()) {
                            formError = "Bank name, holder name, and account number are required."
                            return@Button
                        }
                        viewModel.saveAdminAgentAccount(
                            target.copy(
                                bankName = bankName.trim(),
                                branch = branch.trim(),
                                accountHolder = holder.trim(),
                                accountNumber = accNumber.trim(),
                                method = method
                            )
                        )
                        editingAccount = null
                        Toast.makeText(context, "Bank account saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("SAVE", color = DarkBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAccount = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
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
                        val accNamePart = if (!tx.accountName.isNullOrBlank()) " · Name: ${tx.accountName}" else ""
                        Text(
                            text = "Payout Bank: ${tx.bank} · Acc: ${tx.accountNumber ?: ""}$accNamePart",
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

@Composable
private fun AdminTransactionSummary(
    allTransactions: List<FirestoreTransaction>,
    filteredTransactions: List<FirestoreTransaction>
) {
    val deposits = filteredTransactions.filter { it.type == "DEPOSIT" }
    val withdrawals = filteredTransactions.filter { it.type == "WITHDRAWAL" }
    Card(colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("TRANSACTION OVERVIEW", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
            Text("All accessible requests: ${allTransactions.size} · Filtered: ${filteredTransactions.size}", color = TextSecondary)
            Text("Deposits: ${deposits.size} · LKR %,d".format(deposits.sumOf { it.amount }), color = StatusGreen)
            Text("Withdrawals: ${withdrawals.size} · LKR %,d".format(withdrawals.sumOf { it.amount }), color = NeonCyan)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pending: ${filteredTransactions.count { it.status == "PENDING" }}", color = StatusYellow)
                Text("Processing: ${filteredTransactions.count { it.status == "PROCESSING" }}", color = NeonCyan)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Completed: ${filteredTransactions.count { it.status == "COMPLETED" }}", color = StatusGreen)
                Text("Rejected: ${filteredTransactions.count { it.status == "REJECTED" }}", color = StatusRed)
            }
        }
    }
}

private fun buildAdminCsv(transactions: List<FirestoreTransaction>): String {
    fun cell(value: Any?): String {
        val raw = value?.toString().orEmpty().replace("\r", " ").replace("\n", " ")
        // Prevent spreadsheet formula execution when opening untrusted transaction fields.
        val safe = if (raw.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t')) "'" + raw else raw
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
    val rows = transactions.map { tx ->
        listOf(tx.reference, tx.type, tx.status, tx.amount, tx.playerId,
            tx.paymentMethod, tx.receiptKey, tx.agentAccountLabel, tx.bank,
            tx.rejectionReason, tx.agentNote, tx.userName, tx.userEmail,
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(tx.timestampMillis)))
            .joinToString(",") { cell(it) }
    }
    val header = "Reference,Type,Status,Amount_LKR,Player_ID,Method,Receipt_Reference,Agent_Account,Bank,Rejection_Reason,Agent_Note,Customer_Name,Customer_Email,Created_At"
    return (listOf(header) + rows).joinToString("\r\n")
}
