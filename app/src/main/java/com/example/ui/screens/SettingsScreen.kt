package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.PulseGreen
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val jwtToken by viewModel.jwtToken.collectAsState()
    val context = LocalContext.current

    var showEditProfileModal by remember { mutableStateOf(false) }
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditProfileModal = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulseAvatar(
                        imageUrl = currentUser?.profilePictureUrl ?: "",
                        name = currentUser?.displayName ?: "User",
                        size = 64.dp,
                        isOnline = true
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
                        Text(
                            text = currentUser?.username ?: "@irfankhan",
                            fontSize = 12.sp,
                            color = PulseGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PulseGreen)
                }
            }

            // Theme Options Section
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
                    SettingsRow(icon = Icons.Outlined.Face, title = "Avatar", subtitle = "Create, edit, profile photo") {
                        showEditProfileModal = true
                    }
                    SettingsRow(icon = Icons.Outlined.ChatBubbleOutline, title = "Chats", subtitle = "Theme, wallpapers, chat history", onClick = {})
                    SettingsRow(icon = Icons.Outlined.Notifications, title = "Notifications", subtitle = "Message, group & call tones", onClick = {})
                    SettingsRow(icon = Icons.Outlined.HelpOutline, title = "Help", subtitle = "Help center, contact us, privacy policy", onClick = {})
                }
            }

            // Premium Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPremiumModal = true },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700).copy(alpha = 0.2f))
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
                            Text("Get Premium", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFB8860B))
                            Text("Unlock verified star & exclusive features", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB8860B))
                }
            }

            // Premium Modal
            if (showPremiumModal) {
                var premiumUsername by remember { mutableStateOf("") }
                var premiumPassword by remember { mutableStateOf("") }
                var premiumError by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showPremiumModal = false },
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upgrade to Premium", fontWeight = FontWeight.Bold) 
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Premium costs Rs 50/month. For now, enjoy a 1-month FREE tier!\nGet a verified star (⭐) next to your nickname just like Instagram.",
                                fontSize = 13.sp
                            )
                            
                            OutlinedTextField(
                                value = premiumUsername,
                                onValueChange = { premiumUsername = it; premiumError = "" },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            OutlinedTextField(
                                value = premiumPassword,
                                onValueChange = { premiumPassword = it; premiumError = "" },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                            )

                            if (premiumError.isNotEmpty()) {
                                Text(premiumError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (premiumUsername.isBlank() || premiumPassword.isBlank()) {
                                    premiumError = "Please enter both username and password to agree."
                                } else {
                                    viewModel.upgradeToPremium()
                                    showPremiumModal = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107))
                        ) {
                            Text("Agree & Upgrade", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPremiumModal = false }) {
                            Text("Cancel")
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
