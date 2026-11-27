package com.example.fastcash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.fastcash.data.model.FirestoreNotification
import com.example.fastcash.data.repository.FirestoreFastCashRepository
import com.example.fastcash.ui.components.FastCashTopBar
import com.example.fastcash.ui.components.GoogleAuthHelper
import com.example.fastcash.ui.components.WhatsAppHelper
import com.example.fastcash.ui.screens.*
import com.example.fastcash.ui.theme.*
import com.example.fastcash.ui.viewmodel.FastCashViewModel
import com.example.fastcash.ui.viewmodel.ScreenDestination
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FastCashTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            val viewModel: FastCashViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        val repository = FirestoreFastCashRepository(db)
                        FastCashViewModel(
                            repository = repository,
                            currentUserId = user.uid,
                            currentUserEmail = user.email,
                            currentUserName = user.displayName,
                            isEmailVerified = user.isEmailVerified
                        )
                    }
                }
            )
            FastCashAuthenticatedApp(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastCashAuthenticatedApp(viewModel: FastCashViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    val isNotifOpen by viewModel.isNotificationsSheetOpen.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val allNotifs by viewModel.allNotifications.collectAsState()
    val latestAlert by viewModel.latestUnreadAgentAlert.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val pendingWhatsapp by viewModel.pendingWhatsappAutoLaunch.collectAsState()

    // Automatically open WhatsApp with formatted request when user submits Deposit/Withdrawal
    LaunchedEffect(pendingWhatsapp) {
        val msg = pendingWhatsapp
        if (!msg.isNullOrBlank()) {
            viewModel.consumePendingWhatsappLaunch()
            WhatsAppHelper.openWhatsApp(
                context = context,
                phoneNumber = appConfig.whatsappNumber,
                message = msg
            )
        }
    }

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
                isAdmin = isAdmin,
                isAdminMode = isAdminMode,
                onToggleAdmin = { viewModel.toggleAdminMode() },
                onSignOut = {
                    GoogleAuthHelper.signOut(credentialManager = credentialManager, scope = scope)
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
                if (isAdmin && isAdminMode) {
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
                                text = if (selectedLanguage == "si") {
                                    mapOf(
                                        "Home" to "මුල් පිටුව",
                                        "Deposit" to "තැන්පතු",
                                        "Withdraw" to "ආපසු ගන්න",
                                        "Track" to "සොයන්න",
                                        "Admin" to "නියෝජිත",
                                        "Support" to "සහාය"
                                    )[item.label] ?: item.label
                                } else item.label,
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
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Live in-app alert banner when the agent completes/updates a player's request
            AnimatedVisibility(visible = latestAlert != null) {
                latestAlert?.let { alert ->
                    val bannerColor = when (alert.type) {
                        "COMPLETED" -> StatusGreen
                        "REJECTED" -> StatusRed
                        else -> NeonCyan
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable {
                                viewModel.markNotificationAsRead(alert.id)
                                alert.transactionRef?.let { ref ->
                                    viewModel.trackTransaction(ref)
                                    viewModel.navigateTo(ScreenDestination.TRACK)
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = BorderStroke(1.dp, bannerColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (alert.type == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                                contentDescription = alert.title,
                                tint = bannerColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = bannerColor
                                )
                                Text(
                                    text = alert.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.markNotificationAsRead(alert.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss alert",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
    }

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
                                onClick = {
                                    viewModel.markNotificationAsRead(notif.id)
                                    notif.transactionRef?.let { ref ->
                                        viewModel.isNotificationsSheetOpen.value = false
                                        viewModel.trackTransaction(ref)
                                        viewModel.navigateTo(ScreenDestination.TRACK)
                                    }
                                }
                            )
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
    notification: FirestoreNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(notification.createdAtMillis) {
        val sdf = SimpleDateFormat("HH:mm · MMM dd", Locale.getDefault())
        sdf.format(Date(notification.createdAtMillis))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                0.5.dp,
                if (!notification.read) NeonCyan.copy(alpha = 0.5f) else Color(0x18FFFFFF),
                RoundedCornerShape(4.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.read) DarkSurface else DarkCard
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
                    .background(if (!notification.read) NeonPink else Color(0xFF515469))
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
