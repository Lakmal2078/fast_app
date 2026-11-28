package com.example.fastcash.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    val language by viewModel.selectedLanguage.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val recentTransactions = transactions.take(3)
    val context = LocalContext.current

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
                            text = "Track by reference",
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

        // Telegram Channel, Bot & Web Landing Page Cards
        item {
            TelegramServicesSection(
                language = language,
                onOpenUrl = { url -> openExternalUrl(context, url) }
            )
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

private data class HomeServiceCardData(
    val tag: String,
    val kicker: String,
    val title: String,
    val description: String,
    val buttonLabel: String,
    val url: String,
    val accentColor: Color,
    val icon: ImageVector
)

@Composable
private fun TelegramServicesSection(
    language: String,
    onOpenUrl: (String) -> Unit
) {
    val cards = remember(language) {
        when (language) {
            "si" -> listOf(
                HomeServiceCardData(
                    tag = "home_tips_channel_card",
                    kicker = "DAILY FREE TIPS // @fast_xbet_official_tips",
                    title = "නොමිලේ Sports Betting Tips",
                    description = "දිනපතා විශ්ලේෂණය කළ 100% නොමිලේ ක්‍රීඩා ඉඟි (Free Tips), slip codes, සජීවී odds සහ ක්ෂණික alerts අපගේ නිල Telegram Channel එකෙන් ලබාගන්න.",
                    buttonLabel = "TELEGRAM CHANNEL එකට එක්වන්න",
                    url = "https://t.me/fast_xbet_official_tips",
                    accentColor = NeonCyan,
                    icon = Icons.Default.TipsAndUpdates
                ),
                HomeServiceCardData(
                    tag = "home_telegram_bot_card",
                    kicker = "TELEGRAM BOT // @fast_1xbetcash_bot",
                    title = "Fast xBet Cash Telegram Bot",
                    description = "eZ Cash, mCash, FriMi, iPay සහ බැංකු හරහා මඟපෙන්වන Deposit/Withdraw සහාය, Free Sports Tips සහ භාෂා 3කින්ම සහාය එකම Telegram Bot එකෙන්.",
                    buttonLabel = "TELEGRAM BOT විවෘත කරන්න",
                    url = "https://t.me/fast_1xbetcash_bot?start=app",
                    accentColor = NeonPink,
                    icon = Icons.Default.SmartToy
                ),
                HomeServiceCardData(
                    tag = "home_landing_page_card",
                    kicker = "WEB PORTAL // LIVE TIPS & GUIDE",
                    title = "Fast xBet Cash Web Landing Page",
                    description = "සජීවී Free Tips පෙරදසුන, ගෙවීම් ක්‍රම පිළිබඳ විස්තර, නිතර අසන ප්‍රශ්න (FAQ), QR පිවිසුම සහ Telegram Bot සබැඳි අපගේ වෙබ් පිටුවෙන් බලන්න.",
                    buttonLabel = "වෙබ් පිටුවට පිවිසෙන්න",
                    url = "https://xbet-telegram-bot.lakmal-gamage.workers.dev/",
                    accentColor = NeonCyan,
                    icon = Icons.Default.Language
                )
            )
            "ta" -> listOf(
                HomeServiceCardData(
                    tag = "home_tips_channel_card",
                    kicker = "DAILY FREE TIPS // @fast_xbet_official_tips",
                    title = "இலவச Sports Betting Tips",
                    description = "தினசரி பகுப்பாய்வு செய்யப்பட்ட 100% இலவச விளையாட்டு குறிப்புகள், slip codes, நேரடி odds மற்றும் உடனடி அறிவிப்புகளுக்கு எங்கள் Telegram Channel-ல் இணையுங்கள்.",
                    buttonLabel = "TELEGRAM CHANNEL-ல் இணையுங்கள்",
                    url = "https://t.me/fast_xbet_official_tips",
                    accentColor = NeonCyan,
                    icon = Icons.Default.TipsAndUpdates
                ),
                HomeServiceCardData(
                    tag = "home_telegram_bot_card",
                    kicker = "TELEGRAM BOT // @fast_1xbetcash_bot",
                    title = "Fast xBet Cash Telegram Bot",
                    description = "eZ Cash, mCash, FriMi, iPay மற்றும் வங்கி வழியான Deposit/Withdraw வழிகாட்டல், இலவச Sports Tips மற்றும் மும்மொழி உதவி ஒரே Telegram Bot-ல்.",
                    buttonLabel = "TELEGRAM BOT திறக்கவும்",
                    url = "https://t.me/fast_1xbetcash_bot?start=app",
                    accentColor = NeonPink,
                    icon = Icons.Default.SmartToy
                ),
                HomeServiceCardData(
                    tag = "home_landing_page_card",
                    kicker = "WEB PORTAL // LIVE TIPS & GUIDE",
                    title = "Fast xBet Cash Web Landing Page",
                    description = "நேரடி இலவச Tips முன்னோட்டம், கட்டண முறைகள், FAQ, QR அணுகல் மற்றும் Telegram Bot இணைப்புகளை எங்கள் இணையதளத்தில் காண்க.",
                    buttonLabel = "வலைப்பக்கத்தைத் திறக்கவும்",
                    url = "https://xbet-telegram-bot.lakmal-gamage.workers.dev/",
                    accentColor = NeonCyan,
                    icon = Icons.Default.Language
                )
            )
            else -> listOf(
                HomeServiceCardData(
                    tag = "home_tips_channel_card",
                    kicker = "DAILY FREE TIPS // @fast_xbet_official_tips",
                    title = "Free Sports Betting Tips",
                    description = "100% free daily analyzed sports tips, slip codes, real market odds, and instant alerts via our official Telegram Channel.",
                    buttonLabel = "JOIN FREE TIPS CHANNEL",
                    url = "https://t.me/fast_xbet_official_tips",
                    accentColor = NeonCyan,
                    icon = Icons.Default.TipsAndUpdates
                ),
                HomeServiceCardData(
                    tag = "home_telegram_bot_card",
                    kicker = "TELEGRAM BOT // @fast_1xbetcash_bot",
                    title = "Fast xBet Cash Telegram Bot",
                    description = "Guided eZ Cash, mCash, FriMi, iPay & bank deposits/withdrawals, free sports tips, and multilingual help — all inside one Telegram bot.",
                    buttonLabel = "OPEN TELEGRAM BOT",
                    url = "https://t.me/fast_1xbetcash_bot?start=app",
                    accentColor = NeonPink,
                    icon = Icons.Default.SmartToy
                ),
                HomeServiceCardData(
                    tag = "home_landing_page_card",
                    kicker = "WEB PORTAL // LIVE TIPS & GUIDE",
                    title = "Fast xBet Cash Web Landing Page",
                    description = "Explore today's live tips preview, local payment guides, FAQ, quick QR access, and direct Telegram Bot launcher on our web portal.",
                    buttonLabel = "OPEN WEB LANDING PAGE",
                    url = "https://xbet-telegram-bot.lakmal-gamage.workers.dev/",
                    accentColor = NeonCyan,
                    icon = Icons.Default.Language
                )
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            Text(
                text = "TELEGRAM & TIPS // SERVICES",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (language) {
                    "si" -> "Telegram Bot, Free Tips සහ Web සේවා"
                    "ta" -> "Telegram Bot, இலவச Tips & Web சேவைகள்"
                    else -> "Free Tips, Telegram Bot & Web Portal"
                },
                style = MaterialTheme.typography.headlineMedium
            )
        }

        cards.forEach { card ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(card.tag)
                    .border(1.dp, card.accentColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(1.dp, card.accentColor.copy(alpha = 0.45f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = card.icon,
                                contentDescription = null,
                                tint = card.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.kicker,
                                style = MaterialTheme.typography.labelSmall,
                                color = card.accentColor,
                                fontSize = 10.sp
                            )
                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onOpenUrl(card.url) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        border = BorderStroke(1.dp, card.accentColor.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = card.accentColor),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = card.buttonLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = card.accentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = card.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "Unable to open link: $url", Toast.LENGTH_SHORT).show()
    }
}
