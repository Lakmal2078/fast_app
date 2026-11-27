package com.example.fastcash.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastcash.data.model.TransactionEntity
import com.example.fastcash.data.model.TransactionType
import com.example.fastcash.ui.components.*
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import com.example.fastcash.ui.viewmodel.ScreenDestination
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: FastCashViewModel,
    modifier: Modifier = Modifier
) {
    val content by viewModel.content.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val recentTransactions = transactions.take(3)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Hero Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = content.heroKicker,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content.heroTitle,
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary,
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = content.heroText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Hero action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo(ScreenDestination.DEPOSIT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_deposit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = content.deposit.uppercase(),
                            color = DarkBackground,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(ScreenDestination.WITHDRAW) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_withdraw_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = content.withdraw.uppercase(),
                            color = NeonPink,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Trust row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = content.secure,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "15–30 min review",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        item {
            val message by viewModel.syncError.collectAsState()
            message?.let { Text(it, color = StatusYellow) }
        }

        // "How it works" 3 Steps
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "THE FLOW // 03 STEPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonPink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Clear by default.",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                content.steps.forEachIndexed { index, stepTitle ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "0${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonPink,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stepTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = when (index) {
                                        0 -> "Choose deposit or withdrawal and enter the desired amount."
                                        1 -> "Submit details and attach receipt reference for verification."
                                        else -> "Monitor real-time progress on your interactive timeline."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recent Requests Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT REQUESTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                    Text(
                        text = "VIEW ALL",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPink,
                        modifier = Modifier.clickable {
                            viewModel.navigateTo(ScreenDestination.TRACK)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Text(
                        text = "No requests yet. Start a deposit or withdrawal to begin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentTransactions) { tx ->
                TransactionSummaryCard(
                    transaction = tx,
                    onClick = {
                        viewModel.trackTransaction(tx.reference)
                        viewModel.navigateTo(ScreenDestination.TRACK)
                    }
                )
            }
        }

        // Responsible Gaming Disclaimer
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x33FF2097), RoundedCornerShape(6.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Responsible Gaming",
                        tint = NeonPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "RESPONSIBLE GAMBLING // 18+",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = content.responsible,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionSummaryCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(transaction.createdAt) {
        val sdf = SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.createdAt))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, Color(0x2200E0FF), RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (transaction.type == TransactionType.DEPOSIT)
                                Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (transaction.type == TransactionType.DEPOSIT) NeonCyan else NeonPink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = transaction.reference,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "${transaction.type} · $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LKR %,d".format(transaction.amount),
                        style = MaterialTheme.typography.labelLarge,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusPill(status = transaction.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TimelineView(status = transaction.status)

            if (transaction.rejectionReason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reason: ${transaction.rejectionReason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusRed,
                    fontSize = 11.sp
                )
            }
        }
    }
}
