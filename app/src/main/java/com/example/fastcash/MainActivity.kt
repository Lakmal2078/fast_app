package com.example.fastcash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastcash.data.model.NotificationEntity
import com.example.fastcash.ui.components.FastCashTopBar
import com.example.fastcash.ui.screens.*
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import com.example.fastcash.ui.viewmodel.ScreenDestination
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: FastCashViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FastCashTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val selectedLanguage by viewModel.selectedLanguage.collectAsState()
                val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
                val isNotifOpen by viewModel.isNotificationsSheetOpen.collectAsState()
                val isAdminMode by viewModel.isAdminMode.collectAsState()
                val allNotifs by viewModel.allNotifications.collectAsState()

                // BackHandler to navigate back to Home
                if (currentScreen != ScreenDestination.HOME) {
                    BackHandler {
                        viewModel.navigateTo(ScreenDestination.HOME)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    topBar = {
                        FastCashTopBar(
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = { viewModel.setLanguage(it) },
                            unreadNotificationsCount = unreadCount,
                            onOpenNotifications = { viewModel.isNotificationsSheetOpen.value = true },
                            isAdminMode = isAdminMode,
                            onToggleAdminMode = {
                                viewModel.isAdminMode.value = !isAdminMode
                                if (!isAdminMode) {
                                    viewModel.navigateTo(ScreenDestination.ADMIN)
                                } else if (currentScreen == ScreenDestination.ADMIN) {
                                    viewModel.navigateTo(ScreenDestination.HOME)
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            modifier = Modifier.border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(0.dp))
                        ) {
                            val items = mutableListOf(
                                NavItem("Home", Icons.Default.Home, ScreenDestination.HOME),
                                NavItem("Deposit", Icons.Default.ArrowDownward, ScreenDestination.DEPOSIT),
                                NavItem("Withdraw", Icons.Default.ArrowUpward, ScreenDestination.WITHDRAW),
                                NavItem("Track", Icons.Default.Search, ScreenDestination.TRACK),
                                NavItem("Support", Icons.Default.Headphones, ScreenDestination.SUPPORT)
                            )
                            if (isAdminMode) {
                                items.add(4, NavItem("Admin", Icons.Default.AdminPanelSettings, ScreenDestination.ADMIN))
                            }

                            items.forEach { item ->
                                val isSelected = currentScreen == item.destination
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.navigateTo(item.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = if (isSelected) NeonCyan else TextMuted
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) NeonCyan else TextMuted,
                                            fontSize = 9.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = NeonCyan.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            ScreenDestination.HOME -> HomeScreen(viewModel = viewModel)
                            ScreenDestination.DEPOSIT -> DepositScreen(viewModel = viewModel)
                            ScreenDestination.WITHDRAW -> WithdrawScreen(viewModel = viewModel)
                            ScreenDestination.TRACK -> TrackScreen(viewModel = viewModel)
                            ScreenDestination.ADMIN -> AdminScreen(viewModel = viewModel)
                            ScreenDestination.SUPPORT -> SupportScreen(viewModel = viewModel)
                        }
                    }
                }

                // Notification Bottom Sheet
                if (isNotifOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.isNotificationsSheetOpen.value = false },
                        containerColor = DarkCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NOTIFICATION CENTRE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan
                                )
                                TextButton(
                                    onClick = { viewModel.markAllNotificationsAsRead() }
                                ) {
                                    Text("Mark all read", color = TextMuted, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (allNotifs.isEmpty()) {
                                Text(
                                    text = "No notifications yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxHeight(0.6f)
                                ) {
                                    items(allNotifs) { notif ->
                                        NotificationItemRow(
                                            notification = notif,
                                            onClick = { viewModel.markNotificationAsRead(notif.id) }
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

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val destination: ScreenDestination
)

@Composable
fun NotificationItemRow(
    notification: NotificationEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(notification.createdAt) {
        val sdf = SimpleDateFormat("HH:mm · MMM dd", Locale.getDefault())
        sdf.format(Date(notification.createdAt))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(0.5.dp, if (!notification.isRead) NeonCyan.copy(alpha = 0.5f) else Color(0x18FFFFFF), RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) DarkSurface else DarkCard
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = 5.dp)
                    .clip(CircleShape)
                    .background(if (!notification.isRead) NeonPink else Color(0xFF515469))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }
        }
    }
}
