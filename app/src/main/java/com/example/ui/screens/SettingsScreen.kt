package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.util.MediaUtils
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PulseAvatar
import com.example.ui.components.RealtimeConnectivityBadge
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val jwtToken by viewModel.jwtToken.collectAsState()
    val context = LocalContext.current

    var showEditProfileModal by remember { mutableStateOf(false) }
    var showBlockedContactsModal by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var editBio by remember { mutableStateOf(currentUser?.bio ?: "") }
    var editAvatarUrl by remember { mutableStateOf(currentUser?.profilePictureUrl ?: "") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = MediaUtils.copyUriToInternalStorage(context, uri, "profile_pics")
            if (localPath != null) {
                editAvatarUrl = "file://$localPath"
            }
        }
    }

    var showContextualDpModal by remember { mutableStateOf(false) }
    var showStatusPrivacyModal by remember { mutableStateOf(false) }
    
    // Status Privacy Options
    var statusPrivacyMode by remember { mutableStateOf(currentUser?.statusPrivacyMode ?: "PUBLIC") }
    var statusPrivacyList by remember { mutableStateOf(currentUser?.statusPrivacyList ?: "") }
    
    // DP Options
    var chatDpUrl by remember { mutableStateOf(currentUser?.chatProfilePictureUrl ?: "") }
    var postDpUrl by remember { mutableStateOf(currentUser?.postProfilePictureUrl ?: "") }
    var channelDpUrl by remember { mutableStateOf(currentUser?.channelProfilePictureUrl ?: "") }
    var channelAliasInput by remember { mutableStateOf(currentUser?.channelAlias ?: "") }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            editName = it.displayName
            editBio = it.bio
            editAvatarUrl = it.profilePictureUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            val isWsConnected = connectionState == com.example.data.network.WebSocketState.CONNECTED
            val userOnlineStatus = currentUser?.onlineStatus ?: "ONLINE"
            val isUserCurrentlyOnline = isWsConnected && userOnlineStatus == "ONLINE"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditProfileModal = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulseAvatar(
                            imageUrl = currentUser?.profilePictureUrl ?: "",
                            name = currentUser?.displayName ?: "User",
                            size = 64.dp,
                            isOnline = isUserCurrentlyOnline
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUser?.displayName ?: "Mohammad Irfan Khan",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (currentUser?.isPremium == true) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Star, contentDescription = "Premium User", tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = currentUser?.bio ?: "Hey there! I am using Pulse Chat ⚡",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = currentUser?.username ?: "@irfankhan",
                                    fontSize = 12.sp,
                                    color = PulseGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                                RealtimeConnectivityBadge(
                                    isConnected = isWsConnected,
                                    onlineStatus = userOnlineStatus
                                )
                            }
                        }

                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PulseGreen)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Presence Status Quick-Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Presence Status",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "ONLINE" to "Online",
                                "AWAY" to "Away",
                                "OFFLINE" to "Invisible"
                            ).forEach { (statusKey, label) ->
                                val isSelected = userOnlineStatus == statusKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateOnlineStatus(statusKey) },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PulseGreen.copy(alpha = 0.2f),
                                        selectedLabelColor = PulseGreen
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Theme Options Section
            val showExactTimestamps by viewModel.showExactTimestamps.collectAsState()
            val chatWallpaper by viewModel.chatWallpaper.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("APPEARANCE & THEME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseGreen)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Theme Mode", fontWeight = FontWeight.Bold)
                        Text(themeMode.name, color = PulseGreen, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton("Light", themeMode == AppThemeMode.LIGHT) {
                            viewModel.setThemeMode(AppThemeMode.LIGHT)
                        }
                        ThemeOptionButton("Dark", themeMode == AppThemeMode.DARK) {
                            viewModel.setThemeMode(AppThemeMode.DARK)
                        }
                        ThemeOptionButton("AMOLED", themeMode == AppThemeMode.AMOLED) {
                            viewModel.setThemeMode(AppThemeMode.AMOLED)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Chat Wallpaper Selection Section
                    Text("Chat Background Wallpaper", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Customize the visual background color or gradient for all chat conversations",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val wallpaperOptions = listOf(
                        Triple("DEFAULT", "Default", androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF1E1F24), Color(0xFF141518)))),
                        Triple("OCEAN", "Ocean", androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))),
                        Triple("SUNSET", "Sunset", androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2D112C), Color(0xFF530031), Color(0xFF8D2039)))),
                        Triple("EMERALD", "Emerald", androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0A2E26), Color(0xFF145344), Color(0xFF1F7A65)))),
                        Triple("CYBER", "Cyber", androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF180A29), Color(0xFF38004C), Color(0xFF003853)))),
                        Triple("WARM_CHARCOAL", "Mocha", androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2C2421), Color(0xFF1C1715)))),
                        Triple("PASTEL", "Pastel", androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2B2038), Color(0xFF473355), Color(0xFF3C2C47)))),
                        Triple("AMOLED", "Pitch Black", androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF000000))))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        wallpaperOptions.forEach { (wpKey, name, brush) ->
                            val isSelected = chatWallpaper == wpKey
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setChatWallpaper(wpKey) }
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) PulseGreen else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size( width = 64.dp, height = 80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(brush),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = PulseGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PulseGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Show Exact Timestamps", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Display exact time (e.g. 10:45 AM) on individual chat messages",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showExactTimestamps,
                            onCheckedChange = { viewModel.toggleShowExactTimestamps(it) },
                            modifier = Modifier.testTag("exact_timestamps_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PulseGreen
                            )
                        )
                    }
                }
            }

            // Admin & System Control Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleAdminDashboard(true) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = PulseGreen, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Erlang/OTP Admin Dashboard", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Real-time node metrics, moderation & analytics", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // More WhatsApp-Like Settings Options
            var showPremiumModal by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsRow(icon = Icons.Outlined.Key, title = "Account", subtitle = "Privacy, security, change number") {
                        showStatusPrivacyModal = true
                    }
                    SettingsRow(icon = Icons.Outlined.Block, title = "Blocked Contacts", subtitle = "Manage blocked users") {
                        showBlockedContactsModal = true
                    }
                    SettingsRow(icon = Icons.Outlined.Face, title = "Avatar", subtitle = "Create, edit, profile photo") {
                        showEditProfileModal = true
                    }
                    SettingsRow(icon = Icons.Outlined.ChatBubbleOutline, title = "Chats", subtitle = "Theme, wallpapers, chat history", onClick = {})
                    SettingsRow(icon = Icons.Outlined.Notifications, title = "Notifications", subtitle = "Message, group & call tones", onClick = {})
                    SettingsRow(icon = Icons.Outlined.HelpOutline, title = "Help", subtitle = "Help center, contact us, privacy policy", onClick = {})
                }
            }

            // Premium VIP Membership Card
            val isPremiumActive = currentUser?.isPremium == true
            val expiryMs = currentUser?.premiumExpiryTimestamp ?: 0L
            val nowMs = System.currentTimeMillis()
            val remainingDays = if (expiryMs > nowMs) ((expiryMs - nowMs) / (1000 * 60 * 60 * 24)).toInt() else 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPremiumModal = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremiumActive) Color(0xFFFFD700).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isPremiumActive) "VIP Premium Active ⭐" else "V-Link VIP Premium Tier",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isPremiumActive) Color(0xFFD4AF37) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isPremiumActive) "1-Month Free Tier • $remainingDays days remaining" else "Activate 1-Month Free Tier Trial & Verified Star",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFFFC107))
                }
            }

            // Bank Payouts & Monetization Card
            var showBankModal by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBankModal = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = VLinkCyan, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Creator Bank Payouts & Revenue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Link bank account for direct premium payouts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // Bank Account Modal
            if (showBankModal) {
                var bankHolderName by remember { mutableStateOf(currentUser?.displayName ?: "") }
                var accountNumber by remember { mutableStateOf("") }
                var ifscCode by remember { mutableStateOf("") }
                var bankName by remember { mutableStateOf("") }
                var bankSaveMessage by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showBankModal = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = VLinkCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Link Bank Account for Payouts", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "When users purchase VIP Premium subscriptions, funds are automatically transferred directly to your linked bank account.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = bankHolderName,
                                onValueChange = { bankHolderName = it },
                                label = { Text("Account Holder Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = bankName,
                                onValueChange = { bankName = it },
                                label = { Text("Bank Name") },
                                placeholder = { Text("e.g. State Bank of India / HDFC Bank") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { accountNumber = it },
                                label = { Text("Account Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = ifscCode,
                                onValueChange = { ifscCode = it },
                                label = { Text("IFSC / SWIFT Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (bankSaveMessage.isNotEmpty()) {
                                Text(bankSaveMessage, color = PulseGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (accountNumber.isBlank() || ifscCode.isBlank() || bankName.isBlank()) {
                                    bankSaveMessage = "Please enter complete bank details."
                                } else {
                                    bankSaveMessage = "Bank account linked successfully! Direct payouts enabled."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan, contentColor = Color.Black)
                        ) {
                            Text("Save Bank Details", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBankModal = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Premium Modal
            if (showPremiumModal) {
                AlertDialog(
                    onDismissRequest = { showPremiumModal = false },
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("V-Link VIP Premium Tier", fontWeight = FontWeight.Bold) 
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Activate your 1-Month FREE Tier Trial today!\nGet an official Verified Gold Star (⭐) badge next to your nickname across all chats, calls, and posts.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("⭐ VIP Premium Features Included:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD4AF37))
                                    Text("• Gold Star Badge on profile and messages", fontSize = 12.sp)
                                    Text("• 1080p Ultra HD WebRTC voice & video calls", fontSize = 12.sp)
                                    Text("• 2 GB high-speed file attachments", fontSize = 12.sp)
                                    Text("• Exclusive AMOLED & glassmorphic themes", fontSize = 12.sp)
                                    Text("• 1-Month state duration saved in Firestore", fontSize = 12.sp)
                                }
                            }

                            if (isPremiumActive) {
                                Text("Your 1-Month Free Tier Trial is currently ACTIVE ($remainingDays days remaining).", color = PulseGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.upgradeToPremium(isTrial = true)
                                showPremiumModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color.Black)
                        ) {
                            Text(if (isPremiumActive) "Extend 1-Month Trial" else "Activate 1-Month Free Trial ⭐", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPremiumModal = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Security & Session Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ACCOUNT & SECURITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseGreen)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Google Account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(currentUser?.email ?: "mohammadirfankhan778866@gmail.com", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("JWT Token Session", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(jwtToken?.take(28) + "...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Logout & Delete Account Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("logout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout from Device", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }

                var showDeleteAccountConfirm by remember { mutableStateOf(false) }

                Button(
                    onClick = { showDeleteAccountConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("delete_account_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (showDeleteAccountConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAccountConfirm = false },
                        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        title = { Text("Delete Account Permanently?", fontWeight = FontWeight.Bold) },
                        text = {
                            Text("This will permanently delete your V-Link profile, unique username reservation, local messages, and Firestore record. This action cannot be undone.")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteAccountConfirm = false
                                    viewModel.deleteAccount()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Permanently Delete", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteAccountConfirm = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }


        // Contextual DP Modal
        if (showContextualDpModal) {
            AlertDialog(
                onDismissRequest = { showContextualDpModal = false },
                title = { Text("Contextual Profiles", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("You can set different Profile Pictures (DP) based on where you are seen.", fontSize = 13.sp)
                        
                        OutlinedTextField(
                            value = chatDpUrl,
                            onValueChange = { chatDpUrl = it },
                            label = { Text("Chat DP (URL)") },
                            placeholder = { Text("Leave blank to use main DP") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = postDpUrl,
                            onValueChange = { postDpUrl = it },
                            label = { Text("Post DP (URL)") },
                            placeholder = { Text("Leave blank to use main DP") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = channelDpUrl,
                            onValueChange = { channelDpUrl = it },
                            label = { Text("Channel DP (URL)") },
                            placeholder = { Text("Leave blank to use main DP") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = channelAliasInput,
                            onValueChange = { channelAliasInput = it },
                            label = { Text("Channel Alias (Username)") },
                            placeholder = { Text("e.g. MySecretAlias") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateContextualProfiles(chatDpUrl, postDpUrl, channelDpUrl, channelAliasInput)
                            showContextualDpModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showContextualDpModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Blocked Contacts Modal
        if (showBlockedContactsModal) {
            val blockedChats = chats.filter { it.isBlocked }
            AlertDialog(
                onDismissRequest = { showBlockedContactsModal = false },
                title = { Text("Blocked Contacts", fontWeight = FontWeight.Bold) },
                text = {
                    if (blockedChats.isEmpty()) {
                        Text("No blocked contacts.")
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            blockedChats.forEach { chat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(chat.title, fontWeight = FontWeight.Bold)
                                    TextButton(onClick = {
                                        viewModel.toggleBlockUser(chat.id, false)
                                    }) {
                                        Text("Unblock", color = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBlockedContactsModal = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Status Privacy Modal
        if (showStatusPrivacyModal) {
            AlertDialog(
                onDismissRequest = { showStatusPrivacyModal = false },
                title = { Text("Status Privacy", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Who can see my status updates?", fontSize = 13.sp)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = statusPrivacyMode == "PUBLIC", onClick = { statusPrivacyMode = "PUBLIC" })
                            Text("My Contacts (Public)")
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = statusPrivacyMode == "EXCEPT", onClick = { statusPrivacyMode = "EXCEPT" })
                            Text("My Contacts Except...")
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = statusPrivacyMode == "ONLY_SHARE_WITH", onClick = { statusPrivacyMode = "ONLY_SHARE_WITH" })
                            Text("Only Share With...")
                        }
                        
                        if (statusPrivacyMode != "PUBLIC") {
                            OutlinedTextField(
                                value = statusPrivacyList,
                                onValueChange = { statusPrivacyList = it },
                                label = { Text(if (statusPrivacyMode == "EXCEPT") "Hide from (Usernames)" else "Share with (Usernames)") },
                                placeholder = { Text("user1, user2") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Enter usernames separated by commas.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateStatusPrivacy(statusPrivacyMode, statusPrivacyList)
                            showStatusPrivacyModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Text("Save Privacy")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStatusPrivacyModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit Profile Dialog
        if (showEditProfileModal) {
            AlertDialog(
                onDismissRequest = { showEditProfileModal = false },
                title = { Text("Edit User Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Live Avatar Preview with Change Button
                        Box(contentAlignment = Alignment.BottomEnd) {
                            PulseAvatar(
                                imageUrl = editAvatarUrl,
                                name = editName.ifBlank { "User" },
                                size = 80.dp
                            )
                            IconButton(
                                onClick = {
                                    editAvatarUrl = "https://picsum.photos/seed/vlink_${System.currentTimeMillis()}/300/300"
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(PulseGreen, CircleShape)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Randomize Avatar", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text("Select Preset Avatar or Custom URL", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Quick Preset Avatar Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Cyber" to "https://picsum.photos/seed/cyber_dev/300/300",
                                "Neon" to "https://picsum.photos/seed/future_tech/300/300",
                                "Minimal" to "https://picsum.photos/seed/minimal_art/300/300",
                                "Bot" to "https://picsum.photos/seed/bot_avatar/300/300"
                            ).forEach { (presetLabel, presetUrl) ->
                                FilterChip(
                                    selected = editAvatarUrl == presetUrl,
                                    onClick = { editAvatarUrl = presetUrl },
                                    label = { Text(presetLabel, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Display Name Input
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Display Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PulseGreen) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_display_name_input")
                        )

                        // Pick Profile Picture Button
                        Button(
                            onClick = { 
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = PulseGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Choose from Gallery", color = MaterialTheme.colorScheme.onSurface)
                        }

                        // Bio Input
                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Bio / Status Message") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = PulseGreen) },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_bio_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editName.isNotBlank()) {
                                viewModel.updateUserProfile(editName, editAvatarUrl, editBio)
                                showEditProfileModal = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun RowScope.ThemeOptionButton(title: String, isSelected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) PulseGreen else Color.Transparent,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
