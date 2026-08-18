package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.ChatEntity
import com.example.ui.components.FilterChipGroup
import com.example.ui.components.PulseAvatar
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
import com.example.ui.viewmodels.MainViewModel
import com.example.ui.viewmodels.NavigationTab
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatsScreen(
    viewModel: MainViewModel,
    onOpenNewChatModal: () -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val worldwideResults by viewModel.worldwideSearchResults.collectAsState()
    val isSearchingWorldwide by viewModel.isSearchingWorldwide.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedChatForContextMenu by remember { mutableStateOf<ChatEntity?>(null) }

    val pinnedChats = remember(chats) { chats.filter { it.isPinned } }
    val unpinnedChats = remember(chats) { chats.filter { !it.isPinned } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "V-Link",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PulseGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "E2EE",
                                    tint = PulseGreen,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "E2EE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseGreen
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onOpenNewChatModal() },
                        modifier = Modifier.testTag("camera_icon_button")
                    ) {
                        Icon(Icons.Outlined.CameraAlt, contentDescription = "Camera")
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Group") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenNewChatModal()
                                },
                                leadingIcon = { Icon(Icons.Outlined.Group, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Starred Messages") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.setFilter("All")
                                },
                                leadingIcon = { Icon(Icons.Outlined.Star, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Admin Dashboard") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.toggleAdminDashboard(true)
                                },
                                leadingIcon = { Icon(Icons.Outlined.Analytics, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.selectTab(NavigationTab.SETTINGS)
                                },
                                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                                modifier = Modifier.testTag("menu_settings_item")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenNewChatModal,
                containerColor = PulseGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(Icons.Default.Message, contentDescription = "New Chat")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Prominent Top Search Bar for filtering active conversations by contact name or message content
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotEmpty()) PulseGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = "Search contacts, @handles, or messages...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (searchQuery.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${chats.size} ${if (chats.size == 1) "conversation" else "conversations"} found",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PulseGreen
                    )
                    Text(
                        text = "Filtered by name & content",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilterChipGroup(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            if (chats.isEmpty() && worldwideResults.isEmpty() && !isSearchingWorldwide) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (searchQuery.isNotEmpty()) Icons.Outlined.SearchOff else Icons.Outlined.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching conversations or users" else "No chats found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try searching for another @username worldwide" else "Tap the + message button to start a new conversation",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Worldwide User Directory Search Results
                    if (searchQuery.isNotBlank()) {
                        item {
                            Surface(
                                color = PulseGreen.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = PulseGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "V-LINK USERS WORLDWIDE (${worldwideResults.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PulseGreen
                                    )
                                    if (isSearchingWorldwide) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = PulseGreen)
                                    }
                                }
                            }
                        }

                        if (worldwideResults.isNotEmpty()) {
                            items(worldwideResults, key = { "worldwide_${it.id}" }) { user ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PulseAvatar(imageUrl = user.profilePictureUrl, name = user.displayName, size = 44.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Verified, contentDescription = "Registered", tint = PulseGreen, modifier = Modifier.size(14.dp))
                                            }
                                            Text(
                                                text = if (user.username.startsWith("@")) user.username else "@${user.username}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (user.bio.isNotBlank()) {
                                                Text(
                                                    text = user.bio,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.startChatWithContact(user)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PulseGreen),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else if (!isSearchingWorldwide) {
                            item {
                                Text(
                                    text = "No registered users matching '@${searchQuery.removePrefix("@")}'",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                                )
                            }
                        }

                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                            )
                        }
                    }

                    // Pinned Section
                    if (pinnedChats.isNotEmpty()) {
                        item {
                            Text(
                                text = "PINNED CHATS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseGreen,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        items(pinnedChats, key = { it.id }) { chat ->
                            ChatItemRow(
                                chat = chat,
                                onClick = { viewModel.openChatDetail(chat.id) },
                                onLongClick = { selectedChatForContextMenu = chat }
                            )
                        }
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                            )
                        }
                    }

                    // All Chats Section
                    if (unpinnedChats.isNotEmpty()) {
                        if (pinnedChats.isNotEmpty() || searchQuery.isNotBlank()) {
                            item {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "CONVERSATIONS" else "ALL CHATS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(unpinnedChats, key = { it.id }) { chat ->
                            ChatItemRow(
                                chat = chat,
                                onClick = { viewModel.openChatDetail(chat.id) },
                                onLongClick = { selectedChatForContextMenu = chat }
                            )
                        }
                    }
                }
            }
        }

        // Context Menu Dialog on Long Press
        selectedChatForContextMenu?.let { chat ->
            AlertDialog(
                onDismissRequest = { selectedChatForContextMenu = null },
                title = { Text(chat.title, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text(if (chat.isPinned) "Unpin Chat" else "Pin Chat") },
                            leadingContent = { Icon(Icons.Default.PushPin, contentDescription = null) },
                            modifier = Modifier.clickable {
                                viewModel.togglePinChat(chat.id, !chat.isPinned)
                                selectedChatForContextMenu = null
                            }
                        )
                        ListItem(
                            headlineContent = { Text(if (chat.isArchived) "Unarchive Chat" else "Archive Chat") },
                            leadingContent = { Icon(Icons.Default.Archive, contentDescription = null) },
                            modifier = Modifier.clickable {
                                viewModel.toggleArchiveChat(chat.id, !chat.isArchived)
                                selectedChatForContextMenu = null
                            }
                        )
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = if (chat.isBlocked) "Unblock Contact" else "Block Contact",
                                    color = if (chat.isBlocked) MaterialTheme.colorScheme.primary else Color.Red
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (chat.isBlocked) Icons.Outlined.CheckCircle else Icons.Outlined.Block,
                                    contentDescription = null,
                                    tint = if (chat.isBlocked) MaterialTheme.colorScheme.primary else Color.Red
                                )
                            },
                            modifier = Modifier.clickable {
                                viewModel.toggleBlockUser(chat.id, !chat.isBlocked)
                                selectedChatForContextMenu = null
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedChatForContextMenu = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItemRow(
    chat: ChatEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val formattedTime = remember(chat.lastMessageTimestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(chat.lastMessageTimestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulseAvatar(
            imageUrl = chat.avatarUrl,
            name = chat.title,
            size = 52.dp,
            isOnline = !chat.isGroup
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nick Name Bolded
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (chat.isPremium) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Star, contentDescription = "Premium", tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                    }
                }

                Text(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = if (chat.unreadCount > 0) PulseGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Username handle at the bottom of Nick Name (Instagram-style)
            val handleText = if (chat.username.isNotEmpty()) {
                if (chat.username.startsWith("@")) chat.username else "@${chat.username}"
            } else if (!chat.isGroup) {
                "@${chat.title.lowercase().replace(" ", "_")}"
            } else ""

            if (handleText.isNotEmpty()) {
                Text(
                    text = handleText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = VLinkCyan,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (chat.isBlocked) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Red.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Block,
                                contentDescription = "Blocked",
                                tint = Color.Red,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Blocked Contact",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Red
                            )
                        }
                    }
                } else if (chat.typingStatus.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Typing",
                            tint = PulseGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = chat.typingStatus,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PulseGreen,
                            maxLines = 1
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = chat.lastMessageText.ifEmpty { "End-to-end encrypted message" },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PulseGreen)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
