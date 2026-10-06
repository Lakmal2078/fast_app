package com.example.fastcash.ui.screens

import android.widget.Toast
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
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.ui.components.TimelineView
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import com.example.fastcash.ui.viewmodel.ScreenDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(
    viewModel: FastCashViewModel,
    modifier: Modifier = Modifier
) {
    val amount by viewModel.depositAmount.collectAsState()
    val playerId by viewModel.depositPlayerId.collectAsState()
    val paymentMethod by viewModel.depositPaymentMethod.collectAsState()
    val receiptRef by viewModel.depositReceiptRef.collectAsState()
    val receiptAttached by viewModel.depositReceiptAttached.collectAsState()
    val isSubmitting by viewModel.depositIsSubmitting.collectAsState()
    val successRef by viewModel.depositSuccessRef.collectAsState()
    val error by viewModel.depositError.collectAsState()
    val agentAccounts by viewModel.activeAgentAccounts.collectAsState()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    if (successRef != null) {
        // Success Screen Modal / View
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
                        text = "REQUEST ACCEPTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Signal received.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your request reference is:",
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
                        text = "Keep this reference safe — it is your control key to track agent processing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    TimelineView(status = TransactionStatus.PENDING)

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
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
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "TRACK STATUS",
                            color = DarkBackground,
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
                    text = "DEPOSIT CHANNEL // 01",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Load your player wallet",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Transfer to an active agent account, attach your receipt, and receive an instant tracking key.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // Active Agent Bank Accounts Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ACTIVE AGENT ACCOUNTS // TRANSFER DESTINATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPink
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    agentAccounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurface)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Acc: ${acc.accountNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(acc.accountNumber))
                                    Toast.makeText(context, "Account copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy account number",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
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
                    // Amount (LKR)
                    Column {
                        Text(
                            text = "AMOUNT (LKR) *",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { viewModel.depositAmount.value = it },
                            placeholder = { Text("25,000", color = TextMuted) },
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
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
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
                            text = "1XBET PLAYER ID *",
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
                            text = "PAYMENT METHOD",
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
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = method,
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
                            text = "BANK SLIP / RECEIPT REFERENCE (OPTIONAL)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = receiptRef,
                            onValueChange = { viewModel.depositReceiptRef.value = it },
                            placeholder = { Text("e.g. BOC-FT-889102", color = TextMuted) },
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

                    // Attach Receipt Screenshot
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                1.dp,
                                if (receiptAttached) StatusGreen else NeonCyan.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .background(DarkSurface)
                            .clickable {
                                viewModel.depositReceiptAttached.value = !receiptAttached
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (receiptAttached) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = if (receiptAttached) StatusGreen else NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (receiptAttached) "Receipt Attached (bank_slip.jpg)" else "Attach Receipt Screenshot (Tap to select)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (receiptAttached) StatusGreen else NeonCyan
                            )
                        }
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
                            Text("SUBMITTING…", color = DarkBackground, style = MaterialTheme.typography.labelLarge)
                        } else {
                            Text(
                                text = "SUBMIT SECURE REQUEST",
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
