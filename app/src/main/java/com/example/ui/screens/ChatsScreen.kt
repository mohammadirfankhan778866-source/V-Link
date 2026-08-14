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
    var isSearchActive by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedChatForContextMenu by remember { mutableStateOf<ChatEntity?>(null) }

    val pinnedChats = remember(chats) { chats.filter { it.isPinned } }
    val unpinnedChats = remember(chats) { chats.filter { !it.isPinned } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search chats or messages...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulseGreen,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = "V-Link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.setSearchQuery("")
                        },
                        modifier = Modifier.testTag("search_icon_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

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
                                    // Filter starred
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
            FilterChipGroup(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No chats found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the + message button to start a new conversation",
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
                        if (pinnedChats.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ALL CHATS",
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
                if (chat.typingStatus.isNotEmpty()) {
                    Text(
                        text = chat.typingStatus,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PulseGreen,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = chat.lastMessageText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
