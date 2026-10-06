package com.example.fastcash.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastcash.data.model.TransactionStatus
import com.example.fastcash.ui.theme.*

@Composable
fun StatusPill(status: TransactionStatus, modifier: Modifier = Modifier) {
    val (color, bg) = when (status) {
        TransactionStatus.PENDING -> StatusYellow to Color(0x22FFD15C)
        TransactionStatus.PROCESSING -> NeonCyan to Color(0x2200E0FF)
        TransactionStatus.COMPLETED -> StatusGreen to Color(0x228CFF7B)
        TransactionStatus.REJECTED -> StatusRed to Color(0x22FF487A)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("status_pill_${status.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TimelineView(status: TransactionStatus, modifier: Modifier = Modifier) {
    val stages = listOf("SUBMITTED", "PROCESSING", "COMPLETED")
    val currentStep = when (status) {
        TransactionStatus.PENDING -> 1
        TransactionStatus.PROCESSING -> 2
        TransactionStatus.COMPLETED -> 3
        TransactionStatus.REJECTED -> 1
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, stage ->
            val isActive = index < currentStep
            val isCurrent = index == currentStep - 1

            // Circle step
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isActive) DarkBackground else DarkSurface)
                        .border(
                            1.5.dp,
                            if (isActive) NeonCyan else Color(0xFF343847),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive && index < currentStep - 1) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed step",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) NeonCyan else TextMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stage,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isActive) (if (isCurrent) NeonCyan else TextPrimary) else TextMuted,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Connecting line
            if (index < stages.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .offset(y = (-10).dp)
                        .background(
                            if (index < currentStep - 1) {
                                Brush.horizontalGradient(listOf(NeonCyan, NeonPink))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF343847), Color(0xFF343847)))
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun HudBalanceCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FC / 01",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusGreen.copy(alpha = pulse))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AGENTS ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "NETWORK LIQUIDITY // LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "LKR ",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "486,920",
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 12 Equalizer signal bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(2.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.35f, 0.65f, 0.9f, 0.5f, 1.0f, 0.75f, 0.4f, 0.85f, 0.6f, 0.95f, 0.7f, 0.45f)
                heights.forEachIndexed { idx, barHeight ->
                    val animMultiplier by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600 + (idx * 90), easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bar_$idx"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(barHeight * animMultiplier)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(NeonPink, NeonCyan)
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SECURE WALLET BRIDGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "ENCRYPTED // 256",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonPink
                )
            }
        }
    }
}

@Composable
fun LiveTicker(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NeonCyan.copy(alpha = 0.05f),
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "FAST CASH NETWORK /// WALASMULLA · BELIATTA ROAD 24/7 /// REAL-TIME 1XBET OPS",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastCashTopBar(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    unreadNotificationsCount: Int,
    onOpenNotifications: () -> Unit,
    isAdminMode: Boolean,
    onToggleAdminMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(0.dp)),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground,
            titleContentColor = TextPrimary
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
                        .background(NeonCyan.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Fast Cash Logo",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row {
                        Text(
                            text = "FAST",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "CASH",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeonPink
                        )
                    }
                    Text(
                        text = "LK // AGENT NETWORK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 8.sp
                    )
                }
            }
        },
        actions = {
            // Language selector chips
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp))
                    .padding(2.dp)
            ) {
                listOf("EN", "SI", "TA").forEach { lang ->
                    val isSelected = selectedLanguage.equals(lang, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) NeonCyan else Color.Transparent)
                            .clickable { onLanguageChange(lang.lowercase()) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("lang_btn_$lang"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) DarkBackground else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Notifications button with badge
            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier.testTag("notification_btn")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationsCount > 0) {
                            Badge(
                                containerColor = NeonPink,
                                contentColor = TextPrimary
                            ) {
                                Text("$unreadNotificationsCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = NeonCyan
                    )
                }
            }

            // Admin Toggle Button
            IconButton(
                onClick = onToggleAdminMode,
                modifier = Modifier.testTag("admin_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin Console",
                    tint = if (isAdminMode) NeonPink else TextMuted
                )
            }
        }
    )
}
