package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.AttachmentOptionItem
import com.example.ui.components.DeleteMessageConfirmationDialog
import com.example.ui.components.EmojiPickerPopup
import com.example.ui.components.MessageStatusTicks
import com.example.ui.components.PulseAvatar
import com.example.ui.components.SharedMediaGalleryBottomSheet
import com.example.ui.components.TypingStatusDisplay
import com.example.ui.components.VoiceNotePlayer
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val chatFlow = remember(chatId) { viewModel.repository.getChatById(chatId) }
    val chat by chatFlow.collectAsState(initial = null)

    val messagesFlow = remember(chatId) { viewModel.repository.getMessagesForChat(chatId) }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    val replyingToMessage by viewModel.replyingToMessage.collectAsState()
    val showExactTimestamps by viewModel.showExactTimestamps.collectAsState()
    val chatWallpaper by viewModel.chatWallpaper.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserId = currentUser?.id ?: "usr_google_irfan_9075"

    var inputText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var pendingImageToSend by remember { mutableStateOf<Pair<String, String>?>(null) }
    var imageCaptionInput by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var isSearchingMessages by remember { mutableStateOf(false) }
    var messageSearchQuery by remember { mutableStateOf("") }
    var showReactionPickerForMsg by remember { mutableStateOf<MessageEntity?>(null) }
    var showReactionDetailsForMsg by remember { mutableStateOf<MessageEntity?>(null) }
    var showMsgOptionsForMsg by remember { mutableStateOf<MessageEntity?>(null) }
    var msgToDelete by remember { mutableStateOf<MessageEntity?>(null) }
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    var showChatInfoSheet by remember { mutableStateOf(false) }
    var showMediaGallerySheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Mark messages as read on entry and when new messages arrive
    LaunchedEffect(chatId, messages.size) {
        viewModel.markChatAsRead(chatId)
    }

    // Sync real-time typing status with backend
    LaunchedEffect(inputText) {
        if (inputText.isNotBlank() && editingMessage == null) {
            viewModel.setUserTyping(chatId, true)
        } else {
            viewModel.setUserTyping(chatId, false)
        }
    }

    // Clear typing status on leaving chat
    DisposableEffect(chatId) {
        onDispose {
            viewModel.setUserTyping(chatId, false)
        }
    }

    // Restore draft text from DataStore when entering the chat screen
    LaunchedEffect(chatId) {
        val savedDraft = viewModel.getChatDraftOnce(chatId)
        if (savedDraft.isNotBlank() && inputText.isBlank()) {
            inputText = savedDraft
        }
    }

    // Save draft text to DataStore when the user leaves or types (only if not editing)
    DisposableEffect(chatId, inputText) {
        onDispose {
            if (editingMessage == null) {
                viewModel.saveChatDraft(chatId, inputText)
            }
        }
    }

    // Filter messages based on keyword search query
    val displayMessages = remember(messages, messageSearchQuery) {
        if (messageSearchQuery.isBlank()) {
            messages
        } else {
            messages.filter { msg ->
                msg.content.contains(messageSearchQuery, ignoreCase = true) ||
                        msg.fileName.contains(messageSearchQuery, ignoreCase = true) ||
                        msg.senderName.contains(messageSearchQuery, ignoreCase = true)
            }
        }
    }

    // Group consecutive messages sent by the same user within 2 minutes into single cohesive bubbles
    val messageGroups = remember(displayMessages, currentUserId) {
        groupConsecutiveMessages(displayMessages, currentUserId)
    }

    LaunchedEffect(messageGroups.size) {
        if (messageGroups.isNotEmpty()) {
            listState.animateScrollToItem(messageGroups.size - 1)
        }
    }

    val wallpaperBrush: androidx.compose.ui.graphics.Brush? = when (chatWallpaper) {
        "OCEAN" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
        "SUNSET" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2D112C), Color(0xFF530031), Color(0xFF8D2039)))
        "EMERALD" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0A2E26), Color(0xFF145344), Color(0xFF1F7A65)))
        "CYBER" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF180A29), Color(0xFF38004C), Color(0xFF003853)))
        "WARM_CHARCOAL" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2C2421), Color(0xFF1C1715)))
        "PASTEL" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF2B2038), Color(0xFF473355), Color(0xFF3C2C47)))
        "AMOLED" -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF000000)))
        else -> null
    }

    val wallpaperModifier = if (wallpaperBrush != null) {
        Modifier.background(wallpaperBrush)
    } else {
        Modifier.background(MaterialTheme.colorScheme.background)
    }

    Scaffold(
        topBar = {
            if (isSearchingMessages) {
                // In-Chat Keyword Search Bar
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                isSearchingMessages = false
                                messageSearchQuery = ""
                            },
                            modifier = Modifier.testTag("close_chat_search_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    title = {
                        OutlinedTextField(
                            value = messageSearchQuery,
                            onValueChange = { messageSearchQuery = it },
                            placeholder = { Text("Search messages...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_search_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    },
                    actions = {
                        if (messageSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { messageSearchQuery = "" },
                                modifier = Modifier.testTag("clear_chat_search_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showChatInfoSheet = true }
                                .testTag("chat_header_top_bar")
                        ) {
                            PulseAvatar(
                                imageUrl = chat?.avatarUrl ?: "",
                                name = chat?.title ?: "Chat",
                                size = 40.dp,
                                isOnline = chat?.isGroup == false
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = chat?.title ?: "Chat",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (chat?.isPremium == true) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Star, contentDescription = "Premium", tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                                    }
                                }
                                val handleText = if (!chat?.username.isNullOrEmpty()) {
                                    if (chat?.username!!.startsWith("@")) chat?.username!! else "@${chat?.username}"
                                } else if (chat?.isGroup == false && chat?.title != null) {
                                    "@${chat?.title!!.lowercase().replace(" ", "_")}"
                                } else ""

                                val statusText = if (chat?.isBlocked == true) {
                                    "Blocked"
                                } else if (!chat?.typingStatus.isNullOrEmpty()) {
                                    chat?.typingStatus!!
                                } else if (chat?.isGroup == true) {
                                    "${chat?.memberCount} members"
                                } else if (chat?.isOnline == true) {
                                    "Online"
                                } else if (chat?.lastSeenTimestamp != null && chat?.lastSeenTimestamp!! > 0) {
                                    "last seen at " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(chat?.lastSeenTimestamp!!))
                                } else {
                                    "Offline"
                                }

                                val hasTyping = !chat?.typingStatus.isNullOrEmpty()
                                if (hasTyping) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (chat?.isGroup == true) "${chat?.title}: typing..." else "typing...",
                                            fontSize = 11.sp,
                                            color = PulseGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                        val infiniteTransition = rememberInfiniteTransition(label = "header_typing_dots")
                                        val dotAlpha by infiniteTransition.animateFloat(
                                            initialValue = 0.2f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(600, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "hdot"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(PulseGreen.copy(alpha = dotAlpha))
                                        )
                                    }
                                } else {
                                    val subtitle = if (handleText.isNotEmpty()) "$handleText • $statusText" else statusText
                                    Text(
                                        text = subtitle,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Search in chat button
                        IconButton(
                            onClick = { isSearchingMessages = true },
                            modifier = Modifier.testTag("search_messages_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search Messages")
                        }

                        var showCallDialog by remember { mutableStateOf(false) }
                        
                        if (showCallDialog) {
                            AlertDialog(
                                onDismissRequest = { showCallDialog = false },
                                title = { Text("Call ${chat?.title ?: "Contact"}") },
                                text = { Text("Choose call type:") },
                                confirmButton = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val isGroup = chat?.isGroup == true
                                                if (isGroup) {
                                                    viewModel.startGroupCall(chat?.title ?: "Group", chat?.avatarUrl ?: "", false)
                                                } else {
                                                    viewModel.startCall(chat?.title ?: "Contact", chat?.avatarUrl ?: "", false, chat?.username ?: "")
                                                }
                                                showCallDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Call")
                                        }

                                        Button(
                                            onClick = {
                                                val isGroup = chat?.isGroup == true
                                                if (isGroup) {
                                                    viewModel.startGroupCall(chat?.title ?: "Group", chat?.avatarUrl ?: "", true)
                                                } else {
                                                    viewModel.startCall(chat?.title ?: "Contact", chat?.avatarUrl ?: "", true, chat?.username ?: "")
                                                }
                                                showCallDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                                        ) {
                                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Video Call")
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCallDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        IconButton(
                            onClick = { showMediaGallerySheet = true },
                            modifier = Modifier.testTag("open_shared_media_gallery_button")
                        ) {
                            Icon(Icons.Default.PermMedia, contentDescription = "Shared Media Gallery")
                        }
                        IconButton(onClick = { showCallDialog = true }) {
                            Icon(Icons.Default.Call, contentDescription = "Voice Call")
                        }
                        Box {
                            var menuOpen by remember { mutableStateOf(false) }
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Shared Media Gallery") },
                                    onClick = {
                                        menuOpen = false
                                        showMediaGallerySheet = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.PermMedia, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Wallpapers") },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.setChatWallpaper(if (chatWallpaper == "DEFAULT") "EMERALD" else "DEFAULT")
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Wallpaper, contentDescription = null) }
                                )
                                if (chat?.isGroup == true) {
                                    DropdownMenuItem(
                                        text = { Text(if (chat?.adminsOnlyMode == true) "Allow Public Messages" else "Admins Only Mode") },
                                        onClick = {
                                            menuOpen = false
                                            viewModel.toggleAdminsOnlyMode(chatId, !(chat?.adminsOnlyMode == true))
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null) }
                                    )
                                } else if (chat != null) {
                                    DropdownMenuItem(
                                        text = { Text(if (chat?.isBlocked == true) "Unblock User" else "Block User") },
                                        onClick = {
                                            menuOpen = false
                                            viewModel.toggleBlockUser(chatId, !(chat?.isBlocked == true))
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Clear Messages") },
                                    onClick = {
                                        menuOpen = false
                                        scope.launch { viewModel.repository.clearChatMessages(chatId) }
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(wallpaperModifier)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Results Info Banner
                if (messageSearchQuery.isNotBlank()) {
                    Surface(
                        color = PulseGreen.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Found ${displayMessages.size} matching message(s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseGreen
                            )
                            TextButton(
                                onClick = { messageSearchQuery = "" },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Show All", fontSize = 12.sp, color = PulseGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Pinned Messages Banner
                val pinnedMessages = messages.filter { it.isPinned }
                if (pinnedMessages.isNotEmpty()) {
                    val latestPinned = pinnedMessages.last()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                            .clickable {
                                scope.launch {
                                    val index = messageGroups.indexOfFirst { grp -> grp.messages.any { it.id == latestPinned.id } }
                                    if (index >= 0) listState.animateScrollToItem(index)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pinned Message",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseGreen
                            )
                            Text(
                                text = latestPinned.content,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Messages List (Consecutive Messages Grouped under Single Bubble)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Security Banner
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PulseGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Messages are end-to-end encrypted with Signal Protocol.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(messageGroups, key = { it.groupKey }) { group ->
                        GroupedMessageBubbleRow(
                            group = group,
                            isGroupChat = chat?.isGroup == true,
                            searchKeyword = messageSearchQuery,
                            currentUserId = currentUserId,
                            showExactTimestamps = showExactTimestamps,
                            onMessageLongClick = { msg -> showMsgOptionsForMsg = msg },
                            onReactionClick = { msg -> showReactionDetailsForMsg = msg }
                        )
                    }

                    // Typing Indicator Display in Chat Flow
                    if (!chat?.typingStatus.isNullOrEmpty()) {
                        item {
                            TypingStatusDisplay(
                                userName = chat?.title ?: "Contact",
                                showAvatar = true,
                                avatarUrl = chat?.avatarUrl ?: ""
                            )
                        }
                    }
                }

                // Editing Banner
                editingMessage?.let { editMsg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PulseGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = PulseGreen, modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = "Edit Message",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseGreen
                                )
                                Text(
                                    text = editMsg.content,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                editingMessage = null
                                inputText = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel edit", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Replying Banner
                replyingToMessage?.let { replyMsg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${replyMsg.senderName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseGreen
                            )
                            Text(
                                text = replyMsg.content,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.setReplyingMessage(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel reply")
                        }
                    }
                }

                // Lightweight Emoji Picker Popup above the message input field
                AnimatedVisibility(
                    visible = showEmojiPicker,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    EmojiPickerPopup(
                        onEmojiSelected = { emoji ->
                            inputText += emoji
                        },
                        onBackspace = {
                            if (inputText.isNotEmpty()) {
                                inputText = inputText.dropLast(1)
                            }
                        },
                        onClose = { showEmojiPicker = false }
                    )
                }

                // Message Input Bar
                val isAdmin = chat?.adminIds?.contains(currentUserId) == true
                val isBlocked = chat?.isBlocked == true
                val canSendMessage = !isBlocked && (chat?.isGroup != true || !(chat?.adminsOnlyMode == true) || isAdmin)

                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    if (!canSendMessage) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBlocked) "You blocked this contact" else "Only admins can send messages",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        if (!isRecordingVoiceNote) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text(if (editingMessage != null) "Edit message..." else "Message...", fontSize = 14.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("message_input_field"),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                leadingIcon = {
                                    IconButton(
                                        onClick = { showEmojiPicker = !showEmojiPicker },
                                        modifier = Modifier.testTag("emoji_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.Face,
                                            contentDescription = "Emoji Picker",
                                            tint = if (showEmojiPicker) PulseGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (editingMessage == null) {
                                            IconButton(
                                                onClick = { showAttachmentSheet = true },
                                                modifier = Modifier.testTag("attachment_icon_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachFile,
                                                    contentDescription = "Attach",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (inputText.isBlank()) {
                                                IconButton(onClick = { 
                                                    pendingImageToSend = Pair("https://picsum.photos/seed/camera/800/600", "Camera Photo")
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Default.CameraAlt,
                                                        contentDescription = "Camera",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (isRecordingVoiceNote) {
                            var recordSeconds by remember { mutableIntStateOf(0) }
                            LaunchedEffect(isRecordingVoiceNote) {
                                while (isRecordingVoiceNote) {
                                    kotlinx.coroutines.delay(1000)
                                    recordSeconds++
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(CallEndRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "Recording... %02d:%02d", recordSeconds / 60, recordSeconds % 60),
                                    color = CallEndRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { isRecordingVoiceNote = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Recording", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            IconButton(
                                onClick = {
                                    val durationStr = String.format(Locale.getDefault(), "%02d:%02d", recordSeconds / 60, recordSeconds % 60)
                                    viewModel.sendMessage(
                                        chatId = chatId,
                                        content = "Voice note ($durationStr)",
                                        type = MessageType.VOICE_NOTE,
                                        mediaUrl = "audio_sample.mp3"
                                    )
                                    isRecordingVoiceNote = false
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(PulseGreen, CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice Note", tint = Color.White)
                            }
                        } else if (editingMessage != null) {
                            // Edit message save button
                            IconButton(
                                onClick = {
                                    val currentEditing = editingMessage
                                    if (currentEditing != null && inputText.isNotBlank()) {
                                        viewModel.editMessage(currentEditing.id, chatId, inputText.trim())
                                        editingMessage = null
                                        inputText = ""
                                        viewModel.clearChatDraft(chatId)
                                        showEmojiPicker = false
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(PulseGreen, CircleShape)
                                    .testTag("save_edit_message_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save Edit", tint = Color.White)
                            }
                        } else if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val textToSend = inputText.trim()
                                    viewModel.sendMessage(chatId, textToSend)
                                    inputText = ""
                                    viewModel.clearChatDraft(chatId)
                                    showEmojiPicker = false
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(PulseGreen, CircleShape)
                                    .testTag("send_message_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        } else {
                            var recordStartTime by remember { mutableLongStateOf(0L) }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(VoiceMicAccent)
                                    .testTag("voice_recording_button")
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                recordStartTime = System.currentTimeMillis()
                                                isRecordingVoiceNote = true
                                                val released = tryAwaitRelease()
                                                val elapsedMs = System.currentTimeMillis() - recordStartTime
                                                if (released && isRecordingVoiceNote) {
                                                    if (elapsedMs >= 800L) {
                                                        // Automatically send voice message on press release
                                                        val durationSec = maxOf(1, (elapsedMs / 1000).toInt())
                                                        val durationStr = String.format(Locale.getDefault(), "%02d:%02d", durationSec / 60, durationSec % 60)
                                                        viewModel.sendMessage(
                                                            chatId = chatId,
                                                            content = "Voice note ($durationStr)",
                                                            type = MessageType.VOICE_NOTE,
                                                            mediaUrl = "audio_sample.mp3"
                                                        )
                                                    }
                                                    isRecordingVoiceNote = false
                                                }
                                            },
                                            onTap = {
                                                isRecordingVoiceNote = !isRecordingVoiceNote
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Press and hold to record voice note",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    }
                }
            }

            // Attachment Sheet Modal
            if (showAttachmentSheet) {
                ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Share Media", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                AttachmentOptionItem("Document", Icons.Default.Description, Color(0xFF5F66CD)) {
                                    viewModel.sendMessage(chatId, "Project_Architecture_v2.pdf", MessageType.DOCUMENT, "doc.pdf")
                                    showAttachmentSheet = false
                                }
                                AttachmentOptionItem("Camera", Icons.Default.CameraAlt, Color(0xFFD3396D)) {
                                    pendingImageToSend = Pair("https://picsum.photos/seed/camera/800/600", "Camera Photo")
                                    showAttachmentSheet = false
                                }
                                AttachmentOptionItem("Gallery", Icons.Default.Image, Color(0xFF007BF5)) {
                                    pendingImageToSend = Pair("https://picsum.photos/seed/gallery/800/600", "Shared Image")
                                    showAttachmentSheet = false
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                AttachmentOptionItem("Audio", Icons.Default.Headphones, Color(0xFFF26522)) {
                                    viewModel.sendMessage(chatId, "Voice note (0:15)", MessageType.VOICE_NOTE, "audio.mp3")
                                    showAttachmentSheet = false
                                }
                                AttachmentOptionItem("Location", Icons.Default.LocationOn, Color(0xFF10B981)) {
                                    viewModel.sendMessage(chatId, "📍 Live Location: San Francisco, CA")
                                    showAttachmentSheet = false
                                }
                                AttachmentOptionItem("Contact", Icons.Default.Person, Color(0xFF00A2D3)) {
                                    viewModel.sendMessage(chatId, "Contact: John Doe")
                                    showAttachmentSheet = false
                                }
                            }
                        }
                    }
                }
            }

            // Image Thumbnail Preview Dialog before sending
            pendingImageToSend?.let { (imageUrl, defaultCaption) ->
                AlertDialog(
                    onDismissRequest = { pendingImageToSend = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = PulseGreen)
                            Text("Image Preview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Image preview thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            OutlinedTextField(
                                value = imageCaptionInput,
                                onValueChange = { imageCaptionInput = it },
                                placeholder = { Text("Add a caption...") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = PulseGreen) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("image_caption_input"),
                                shape = RoundedCornerShape(16.dp),
                                maxLines = 3
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val caption = imageCaptionInput.trim().ifBlank { defaultCaption.ifBlank { "Shared Image" } }
                                viewModel.sendMessage(
                                    chatId = chatId,
                                    content = caption,
                                    type = MessageType.IMAGE,
                                    mediaUrl = imageUrl
                                )
                                pendingImageToSend = null
                                imageCaptionInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PulseGreen),
                            modifier = Modifier.testTag("confirm_send_image_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            pendingImageToSend = null
                            imageCaptionInput = ""
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Message Options Alert Dialog
            showMsgOptionsForMsg?.let { msg ->
                AlertDialog(
                    onDismissRequest = { showMsgOptionsForMsg = null },
                    title = { Text("Message Actions") },
                    text = {
                        Column {
                            // Quick Reactions Bar
                            Text(
                                text = "Quick React:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val emojisList = listOf("❤️", "👍", "👎", "😂", "😮", "😢", "🙏", "🎉")
                                emojisList.forEach { emoji ->
                                    Text(
                                        text = emoji,
                                        fontSize = 26.sp,
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.addReaction(msg.id, emoji)
                                                showMsgOptionsForMsg = null
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            ListItem(
                                headlineContent = { Text("Reply") },
                                leadingContent = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    viewModel.setReplyingMessage(msg)
                                    showMsgOptionsForMsg = null
                                }
                            )
                            ListItem(
                                headlineContent = { Text(if (msg.isPinned) "Unpin Message" else "Pin Message") },
                                leadingContent = { Icon(Icons.Default.PushPin, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    viewModel.togglePinMessage(msg.id, !msg.isPinned)
                                    showMsgOptionsForMsg = null
                                }
                            )
                            ListItem(
                                headlineContent = { Text(if (msg.isStarred) "Unstar Message" else "Star Message") },
                                leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    viewModel.toggleStarMessage(msg.id, !msg.isStarred)
                                    showMsgOptionsForMsg = null
                                }
                            )
                            if (msg.senderId == currentUserId && msg.type == MessageType.TEXT.name) {
                                ListItem(
                                    headlineContent = { Text("Edit Message") },
                                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = PulseGreen) },
                                    modifier = Modifier.clickable {
                                        editingMessage = msg
                                        inputText = msg.content
                                        showMsgOptionsForMsg = null
                                    }
                                )
                            }
                            ListItem(
                                headlineContent = { Text("View Reactions") },
                                leadingContent = { Icon(Icons.Default.Face, contentDescription = null, tint = PulseGreen) },
                                modifier = Modifier.clickable {
                                    showReactionDetailsForMsg = msg
                                    showMsgOptionsForMsg = null
                                }
                            )
                            ListItem(
                                headlineContent = { Text("Delete for Me") },
                                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    msgToDelete = msg
                                    showMsgOptionsForMsg = null
                                }
                            )
                            if (msg.senderId == currentUserId) {
                                ListItem(
                                    headlineContent = { Text("Delete for Everyone") },
                                    leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red) },
                                    modifier = Modifier.clickable {
                                        msgToDelete = msg
                                        showMsgOptionsForMsg = null
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMsgOptionsForMsg = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Message Delete Confirmation Dialog
            msgToDelete?.let { msg ->
                DeleteMessageConfirmationDialog(
                    isOutgoing = msg.senderId == currentUserId,
                    onDismiss = { msgToDelete = null },
                    onDeleteForMe = { viewModel.deleteForMe(msg.id) },
                    onDeleteForEveryone = if (msg.senderId == currentUserId) {
                        { viewModel.deleteForEveryone(msg.id) }
                    } else null
                )
            }

            // Message Reactions Detail Dialog
            showReactionDetailsForMsg?.let { msg ->
                val reactionsList = remember(msg.reactions) { parseReactions(msg.reactions) }
                AlertDialog(
                    onDismissRequest = { showReactionDetailsForMsg = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null, tint = PulseGreen)
                            Text("Reactions", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (reactionsList.isEmpty()) {
                                Text(
                                    text = "No reactions yet. Be the first to react!",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(reactionsList) { reaction ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                PulseAvatar(
                                                    imageUrl = "",
                                                    name = reaction.userName,
                                                    size = 32.dp
                                                )
                                                Column {
                                                    Text(
                                                        text = reaction.userName,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.sp
                                                    )
                                                    if (reaction.userId == currentUserId) {
                                                        Text(
                                                            text = "You",
                                                            fontSize = 10.sp,
                                                            color = PulseGreen,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(text = reaction.emoji, fontSize = 20.sp)
                                                if (reaction.userId == currentUserId) {
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.removeUserReaction(msg.id, currentUserId)
                                                            showReactionDetailsForMsg = null
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remove reaction",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Add / Change your reaction:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            val emojisList = listOf("❤️", "👍", "👎", "😂", "😮", "😢", "🙏", "🎉")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                emojisList.forEach { emoji ->
                                    val isSelected = reactionsList.any { it.userId == currentUserId && it.emoji == emoji }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .clickable {
                                                viewModel.addReaction(msg.id, emoji)
                                                showReactionDetailsForMsg = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showReactionDetailsForMsg = null }) {
                            Text("Close")
                        }
                    }
                )
            }

            // SHARED MEDIA GALLERY SHEET
            if (showMediaGallerySheet) {
                SharedMediaGalleryBottomSheet(
                    messages = messages,
                    onDismiss = { showMediaGallerySheet = false }
                )
            }

            // GROUP / CONTACT INFO SHEET (WhatsApp-style Header Detail View)
            if (showChatInfoSheet && chat != null) {
                var showAddMemberDialog by remember { mutableStateOf(false) }
                val allContacts by viewModel.repository.allContacts.collectAsState(initial = emptyList())

                ModalBottomSheet(
                    onDismissRequest = { showChatInfoSheet = false },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    modifier = Modifier.testTag("chat_info_bottom_sheet")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Avatar, Title, Handle
                        Spacer(modifier = Modifier.height(8.dp))
                        PulseAvatar(
                            imageUrl = chat?.avatarUrl ?: "",
                            name = chat?.title ?: "Chat",
                            size = 80.dp,
                            isOnline = chat?.isGroup == false
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = chat?.title ?: "Chat",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (chat?.isPremium == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "VIP Premium",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        val handleText = if (!chat?.username.isNullOrEmpty()) {
                            if (chat?.username!!.startsWith("@")) chat?.username!! else "@${chat?.username}"
                        } else if (chat?.isGroup == false && chat?.title != null) {
                            "@${chat?.title!!.lowercase().replace(" ", "_")}"
                        } else "Group ID: ${chat?.id}"

                        Text(
                            text = handleText,
                            fontSize = 13.sp,
                            color = VLinkCyan,
                            fontWeight = FontWeight.Medium
                        )

                        if (chat?.isGroup == true) {
                            Text(
                                text = "Group • ${chat?.memberCount} members",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = if (chat?.isOnline == true) "Online" else "Offline",
                                fontSize = 12.sp,
                                color = if (chat?.isOnline == true) PulseGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Call, Video Call, Search
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Surface(
                                onClick = {
                                    showChatInfoSheet = false
                                    viewModel.startCall(chat?.title ?: "", chat?.avatarUrl ?: "", false, chat?.username ?: "")
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = VLinkCyan)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Audio", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Surface(
                                onClick = {
                                    showChatInfoSheet = false
                                    viewModel.startCall(chat?.title ?: "", chat?.avatarUrl ?: "", true, chat?.username ?: "")
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = VLinkViolet)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Video", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Surface(
                                onClick = {
                                    showChatInfoSheet = false
                                    isSearchingMessages = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = PulseGreen)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Search", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // GROUP DETAILS & MEMBER MANAGEMENT
                        if (chat?.isGroup == true) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${chat?.memberCount} Group Members",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = { showAddMemberDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VLinkCyan,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("add_member_button")
                                    ) {
                                        Icon(
                                            Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Member", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Current user row (Admin)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            PulseAvatar(
                                                imageUrl = currentUser?.profilePictureUrl ?: "https://picsum.photos/seed/irfan/300/300",
                                                name = currentUser?.displayName ?: "You",
                                                size = 40.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = "You", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = VLinkViolet.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "Group Admin",
                                                            fontSize = 10.sp,
                                                            color = VLinkViolet,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = currentUser?.username ?: "@you",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                                        // Contacts list
                                        allContacts.take(5).forEach { member ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                PulseAvatar(
                                                    imageUrl = member.profilePictureUrl,
                                                    name = member.displayName,
                                                    size = 40.dp
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = member.displayName,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = member.username.ifEmpty { "@${member.displayName.lowercase().replace(" ", "_")}" },
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "Member",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // INDIVIDUAL CONTACT DETAILS
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("About & Phone Number", fontSize = 12.sp, color = VLinkCyan, fontWeight = FontWeight.Bold)
                                        Text(text = "Connecting via V-Link ⚡", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text(text = handleText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // ADD MEMBER DIALOG
                if (showAddMemberDialog) {
                    var selectedMembers by remember { mutableStateOf(setOf<String>()) }
                    var memberSearchQuery by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showAddMemberDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = VLinkCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Members to Group", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Select contacts or enter handle to add to ${chat?.title}:", fontSize = 13.sp)

                                OutlinedTextField(
                                    value = memberSearchQuery,
                                    onValueChange = { memberSearchQuery = it },
                                    placeholder = { Text("Search by name or @username...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("add_member_search_input")
                                )

                                val filteredContacts = remember(allContacts, memberSearchQuery) {
                                    if (memberSearchQuery.isBlank()) allContacts
                                    else allContacts.filter {
                                        it.displayName.contains(memberSearchQuery, ignoreCase = true) ||
                                        it.username.contains(memberSearchQuery, ignoreCase = true)
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (filteredContacts.isEmpty() && memberSearchQuery.isNotBlank()) {
                                        val customHandle = if (memberSearchQuery.startsWith("@")) memberSearchQuery else "@$memberSearchQuery"
                                        Surface(
                                            onClick = {
                                                selectedMembers = if (selectedMembers.contains(customHandle)) {
                                                    selectedMembers - customHandle
                                                } else {
                                                    selectedMembers + customHandle
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selectedMembers.contains(customHandle)) VLinkCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Add \"$customHandle\"", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Checkbox(
                                                    checked = selectedMembers.contains(customHandle),
                                                    onCheckedChange = null
                                                )
                                            }
                                        }
                                    } else {
                                        filteredContacts.forEach { contact ->
                                            val isSelected = selectedMembers.contains(contact.displayName)
                                            Surface(
                                                onClick = {
                                                    selectedMembers = if (isSelected) {
                                                        selectedMembers - contact.displayName
                                                    } else {
                                                        selectedMembers + contact.displayName
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) VLinkCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    PulseAvatar(imageUrl = contact.profilePictureUrl, name = contact.displayName, size = 36.dp)
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(contact.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                        Text(
                                                            contact.username.ifEmpty { "@${contact.displayName.lowercase().replace(" ", "_")}" },
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = null
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (selectedMembers.isNotEmpty()) {
                                        viewModel.addMembersToGroup(chat?.id ?: chatId, selectedMembers.toList())
                                    }
                                    showAddMemberDialog = false
                                },
                                enabled = selectedMembers.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan, contentColor = Color.Black),
                                modifier = Modifier.testTag("confirm_add_members_btn")
                            ) {
                                Text("Add (${selectedMembers.size}) Members", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddMemberDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleRow(
    message: MessageEntity,
    isOutgoing: Boolean,
    currentUserId: String,
    showExactTimestamps: Boolean = true,
    onLongClick: () -> Unit,
    onReactionClick: () -> Unit
) {
    val bubbleColor = if (isOutgoing) {
        if (isSystemInDarkTheme()) DarkOutgoingBubble else LightOutgoingBubble
    } else {
        if (isSystemInDarkTheme()) DarkIncomingBubble else LightIncomingBubble
    }

    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    val formattedTime = remember(message.timestamp, showExactTimestamps) {
        val pattern = if (showExactTimestamps) "h:mm:ss a" else "h:mm a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOutgoing) 16.dp else 2.dp,
                            bottomEnd = if (isOutgoing) 2.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = onLongClick
                    )
                    .padding(10.dp)
            ) {
                Column {
                    // Quoted Reply Block
                    if (!message.replyToContent.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.08f))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = message.replyToSenderName ?: "Reply",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseGreen
                                )
                                Text(
                                    text = message.replyToContent!!,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Image Content
                    if (message.type == MessageType.IMAGE.name && message.mediaUrl.isNotBlank()) {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Image preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Voice Note Content
                    if (message.type == MessageType.VOICE_NOTE.name) {
                        var isPlaying by remember { mutableStateOf(false) }
                        VoiceNotePlayer(
                            duration = "0:14",
                            isPlaying = isPlaying,
                            onTogglePlay = { isPlaying = !isPlaying }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Document Content
                    if (message.type == MessageType.DOCUMENT.name) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.08f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = PulseGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(message.content, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Text Message Body
                    if (message.type == MessageType.TEXT.name || message.type == MessageType.IMAGE.name) {
                        Text(
                            text = message.content,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Footer Ticks & Time
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.isStarred) {
                            Icon(Icons.Default.Star, contentDescription = "Starred", tint = Color(0xFFFFB800), modifier = Modifier.size(12.dp))
                        }
                        if (message.isEdited) {
                            Text(
                                text = "edited",
                                fontSize = 9.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = formattedTime,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (isOutgoing) {
                            MessageStatusTicks(status = message.status)
                        }
                    }
                }
            }

            // Display reactions below the message bubble
            val parsedReactions = remember(message.reactions) { parseReactions(message.reactions) }
            if (parsedReactions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp, start = if (isOutgoing) 0.dp else 4.dp, end = if (isOutgoing) 4.dp else 0.dp)
                ) {
                    MessageReactionsLayout(
                        reactions = parsedReactions,
                        currentUserId = currentUserId,
                        onReactionClick = onReactionClick
                    )
                }
            }
        }
    }
}

data class MessageGroup(
    val groupKey: String,
    val senderId: String,
    val senderName: String,
    val isOutgoing: Boolean,
    val messages: List<MessageEntity>
)

fun groupConsecutiveMessages(messages: List<MessageEntity>, currentUserId: String): List<MessageGroup> {
    if (messages.isEmpty()) return emptyList()

    val groups = mutableListOf<MessageGroup>()
    var currentGroupMessages = mutableListOf<MessageEntity>()
    var currentSenderId: String? = null
    var lastTimestamp: Long = 0L

    // Max 2 minutes (120,000 ms) to group consecutive messages together
    val groupingThresholdMs = 120_000L

    for (msg in messages) {
        val sameSender = msg.senderId == currentSenderId
        val withinTime = (msg.timestamp - lastTimestamp) <= groupingThresholdMs

        if (currentGroupMessages.isEmpty() || (sameSender && withinTime)) {
            currentGroupMessages.add(msg)
            currentSenderId = msg.senderId
            lastTimestamp = msg.timestamp
        } else {
            val isOut = currentSenderId == currentUserId
            val firstMsg = currentGroupMessages.first()
            groups.add(
                MessageGroup(
                    groupKey = "${firstMsg.id}_${currentGroupMessages.size}",
                    senderId = currentSenderId ?: "",
                    senderName = firstMsg.senderName,
                    isOutgoing = isOut,
                    messages = currentGroupMessages.toList()
                )
            )
            currentGroupMessages = mutableListOf(msg)
            currentSenderId = msg.senderId
            lastTimestamp = msg.timestamp
        }
    }

    if (currentGroupMessages.isNotEmpty()) {
        val isOut = currentSenderId == currentUserId
        val firstMsg = currentGroupMessages.first()
        groups.add(
            MessageGroup(
                groupKey = "${firstMsg.id}_${currentGroupMessages.size}",
                senderId = currentSenderId ?: "",
                senderName = firstMsg.senderName,
                isOutgoing = isOut,
                messages = currentGroupMessages.toList()
            )
        )
    }

    return groups
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedMessageBubbleRow(
    group: MessageGroup,
    isGroupChat: Boolean,
    searchKeyword: String,
    currentUserId: String,
    showExactTimestamps: Boolean = true,
    onMessageLongClick: (MessageEntity) -> Unit,
    onReactionClick: (MessageEntity) -> Unit
) {
    val isOutgoing = group.isOutgoing
    val bubbleColor = if (isOutgoing) {
        if (isSystemInDarkTheme()) DarkOutgoingBubble else LightOutgoingBubble
    } else {
        if (isSystemInDarkTheme()) DarkIncomingBubble else LightIncomingBubble
    }

    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            // Group Sender Name (shown only for incoming messages in group chats)
            if (!isOutgoing && isGroupChat && group.senderName.isNotBlank()) {
                Text(
                    text = group.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PulseGreen,
                    modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                )
            }

            // Single cohesive bubble housing consecutive messages
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOutgoing) 16.dp else 4.dp,
                            bottomEnd = if (isOutgoing) 4.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    group.messages.forEachIndexed { index, message ->
                        val formattedTime = remember(message.timestamp, showExactTimestamps) {
                            val pattern = if (showExactTimestamps) "h:mm:ss a" else "h:mm a"
                            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                            sdf.format(Date(message.timestamp))
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = { },
                                    onLongClick = { onMessageLongClick(message) }
                                )
                                .padding(vertical = 2.dp)
                        ) {
                            // Quoted Reply Block
                            if (!message.replyToContent.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.08f))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = message.replyToSenderName ?: "Reply",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PulseGreen
                                        )
                                        Text(
                                            text = message.replyToContent!!,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Image Content
                            if (message.type == MessageType.IMAGE.name && message.mediaUrl.isNotBlank()) {
                                AsyncImage(
                                    model = message.mediaUrl,
                                    contentDescription = "Image preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Voice Note Content
                            if (message.type == MessageType.VOICE_NOTE.name) {
                                var isPlaying by remember { mutableStateOf(false) }
                                VoiceNotePlayer(
                                    duration = "0:14",
                                    isPlaying = isPlaying,
                                    onTogglePlay = { isPlaying = !isPlaying }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Document Content
                            if (message.type == MessageType.DOCUMENT.name) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.08f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = PulseGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(message.content, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Text Message Body (with search highlighting)
                            if (message.type == MessageType.TEXT.name || (message.type == MessageType.IMAGE.name && message.content.isNotBlank() && message.content != "Camera Photo")) {
                                HighlightedMessageText(
                                    text = message.content,
                                    keyword = searchKeyword
                                )
                            }

                            // Timestamp, Edited tag, and Status Ticks for each item in the grouped bubble
                            Row(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (message.isStarred) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Starred",
                                        tint = Color(0xFFFFB800),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                if (message.isEdited) {
                                    Text(
                                        text = "edited",
                                        fontSize = 9.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = formattedTime,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                if (isOutgoing) {
                                    MessageStatusTicks(status = message.status)
                                }
                            }

                            // Message Reactions below individual message
                            val parsedReactions = remember(message.reactions) { parseReactions(message.reactions) }
                            if (parsedReactions.isNotEmpty()) {
                                Box(modifier = Modifier.padding(top = 2.dp)) {
                                    MessageReactionsLayout(
                                        reactions = parsedReactions,
                                        currentUserId = currentUserId,
                                        onReactionClick = { onReactionClick(message) }
                                    )
                                }
                            }
                        }

                        // Divider between consecutive messages in the same bubble
                        if (index < group.messages.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightedMessageText(
    text: String,
    keyword: String
) {
    if (keyword.isBlank() || !text.contains(keyword, ignoreCase = true)) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )
        return
    }

    val annotatedString = remember(text, keyword) {
        buildAnnotatedString {
            var startIndex = 0
            val lowerText = text.lowercase(Locale.getDefault())
            val lowerKeyword = keyword.lowercase(Locale.getDefault())
            val keywordLength = keyword.length

            while (startIndex < text.length) {
                val matchIndex = lowerText.indexOf(lowerKeyword, startIndex)
                if (matchIndex == -1) {
                    append(text.substring(startIndex))
                    break
                }

                if (matchIndex > startIndex) {
                    append(text.substring(startIndex, matchIndex))
                }

                withStyle(
                    style = SpanStyle(
                        background = Color(0xFFFFD54F),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(text.substring(matchIndex, matchIndex + keywordLength))
                }

                startIndex = matchIndex + keywordLength
            }
        }
    }

    Text(
        text = annotatedString,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 18.sp
    )
}

data class UIMessageReaction(
    val userId: String,
    val emoji: String,
    val userName: String
)

fun parseReactions(reactionsStr: String): List<UIMessageReaction> {
    if (reactionsStr.isBlank()) return emptyList()
    return reactionsStr.split(",").mapNotNull { part ->
        val subParts = part.split(":")
        if (subParts.size >= 2) {
            val userId = subParts[0]
            val emoji = subParts[1]
            val userName = if (subParts.size >= 3) subParts.drop(2).joinToString(":") else "User"
            UIMessageReaction(userId, emoji, userName)
        } else if (part.isNotBlank()) {
            UIMessageReaction("legacy", part, "User")
        } else {
            null
        }
    }
}

@Composable
fun MessageReactionsLayout(
    reactions: List<UIMessageReaction>,
    currentUserId: String,
    onReactionClick: () -> Unit
) {
    if (reactions.isEmpty()) return

    val grouped = remember(reactions) {
        reactions.groupBy { it.emoji }
    }

    Row(
        modifier = Modifier
            .clickable { onReactionClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        grouped.forEach { (emoji, list) ->
            val hasUserReacted = list.any { it.userId == currentUserId }
            val badgeBg = if (hasUserReacted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            }
            val badgeTextColor = if (hasUserReacted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = emoji, fontSize = 12.sp)
                    Text(
                        text = list.size.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }
        }
    }
}
