package com.example.fastcash.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastcash.R
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.ui.components.SlipImageHelper
import com.example.fastcash.ui.components.StatusPill
import com.example.fastcash.ui.components.TimelineView
import com.example.fastcash.ui.components.transactionLabels
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackScreen(
    viewModel: FastCashViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.selectedLanguage.collectAsState()
    val labels = transactionLabels(language)
    val searchQuery by viewModel.trackSearchQuery.collectAsState()
    val trackedResult by viewModel.trackedResult.collectAsState()
    val isSearched by viewModel.isTrackSearched.collectAsState()
    val allTransactions by viewModel.playerFirestoreTransactions.collectAsState()

    val profileName by viewModel.profileName.collectAsState()
    val profilePlayerId by viewModel.profilePlayerId.collectAsState()
    val isSavingProfile by viewModel.isSavingProfile.collectAsState()

    val context = LocalContext.current

    // Keep trackedResult in sync with live Firestore updates
    val liveTracked = remember(trackedResult, allTransactions) {
        val current = trackedResult ?: return@remember null
        allTransactions.firstOrNull { it.reference == current.reference } ?: current
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Track Header & Search Bar
        item {
            Column {
                Text(
                    text = "CUSTOMER SPACE // REAL-TIME RADAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = labels.getString(R.string.tx_track_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = labels.getString(R.string.tx_track_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.trackSearchQuery.value = it },
                        placeholder = { Text("Paste reference (e.g. TXN-7F42A)", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("track_reference_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Button(
                        onClick = { viewModel.trackTransaction(searchQuery) },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("track_search_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DarkBackground
                        )
                    }
                }
            }
        }

        item {
            val message by viewModel.trackingMessage.collectAsState()
            message?.let { Text(it, color = NeonCyan, style = MaterialTheme.typography.bodyMedium) }
        }

        // Search Result Card
        if (isSearched) {
            item {
                if (liveTracked != null) {
                    val tx = liveTracked
                    val dateStr = remember(tx.timestampMillis) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        sdf.format(Date(tx.timestampMillis))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = labels.getString(R.string.tx_found),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusGreen
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tx.reference,
                                        fontSize = 20.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                StatusPill(status = tx.parsedStatus)
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0x22FFFFFF))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("TYPE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(tx.type, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("AMOUNT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("LKR %,d".format(tx.amount), style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("PLAYER ID", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(tx.playerId, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("TIMESTAMP", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(dateStr, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                }
                            }

                            if (!tx.agentAccountLabel.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("DEPOSITED TO", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    "${tx.agentAccountLabel} (${tx.agentAccountNumber ?: ""})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }

                            if (!tx.bank.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("RECIPIENT BANK", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                val accNamePart = if (!tx.accountName.isNullOrBlank()) " · ${tx.accountName}" else ""
                                Text(
                                    "${tx.bank} (Acc: ${tx.accountNumber ?: ""}$accNamePart)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }

                            val slipBitmap = remember(tx.id, tx.slipImageBase64) {
                                SlipImageHelper.decodeBase64ToImageBitmap(tx.slipImageBase64)
                            }
                            if (slipBitmap != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("UPLOADED SLIP", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = slipBitmap,
                                    contentDescription = "Uploaded deposit slip",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            TimelineView(status = tx.parsedStatus)

                            if (!tx.agentNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = StatusGreen.copy(alpha = 0.14f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "AGENT CONFIRMATION NOTE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusGreen
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tx.agentNote,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }

                            if (tx.parsedStatus == TransactionStatus.REJECTED && !tx.rejectionReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0x22FF487A))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "DECISION NOTE // REJECTION REASON",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StatusRed
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tx.rejectionReason,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = labels.getString(R.string.tx_missing),
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == "si") {
                                    "යොමු අංකය පරීක්ෂා කර නැවත සොයන්න."
                                } else {
                                    "No request matching reference \"$searchQuery\" was found. Please check your reference code or contact support."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Customer Profile Card (Save details to prefill)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonPink.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CUSTOMER PROFILE // SAVED DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Move faster next time.",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Save your name and 1xBet Player ID to prefill future transactions automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { viewModel.profileName.value = it },
                        placeholder = { Text("Full Name", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPink,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = profilePlayerId,
                        onValueChange = { viewModel.profilePlayerId.value = it },
                        placeholder = { Text("1xBet Player ID", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_player_id_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPink,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.saveProfile()
                            Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isSavingProfile && profileName.isNotBlank() && profilePlayerId.trim().length >= 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("profile_save_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isSavingProfile) "SAVING…" else "SAVE PROFILE",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Full Request History
        item {
            Column {
                Text(
                    text = labels.getString(R.string.tx_history),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(allTransactions) { tx ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.trackTransaction(tx.reference) }
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
                            text = tx.reference,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${tx.type} · Player: ${tx.playerId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        if (!tx.agentNote.isNullOrBlank()) {
                            Text(
                                text = tx.agentNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusGreen,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LKR %,d".format(tx.amount),
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusPill(status = tx.parsedStatus)
                    }
                }
            }
        }
    }
}
