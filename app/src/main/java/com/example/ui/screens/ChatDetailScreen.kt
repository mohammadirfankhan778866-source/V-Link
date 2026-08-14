package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.*
import com.example.ui.components.MessageStatusTicks
import com.example.ui.components.PulseAvatar
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
    val currentUser by viewModel.currentUser.collectAsState()
    val currentUserId = currentUser?.id ?: "usr_google_irfan_9075"

    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showReactionPickerForMsg by remember { mutableStateOf<MessageEntity?>(null) }
    var showReactionDetailsForMsg by remember { mutableStateOf<MessageEntity?>(null) }
    var showMsgOptionsForMsg by remember { mutableStateOf<MessageEntity?>(null) }
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    var selectedWallpaper by remember { mutableStateOf("DEFAULT") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val wallpaperBackground = when (selectedWallpaper) {
        "DARK" -> DarkBackground
        "AMOLED" -> AmoledBackground
        "EMERALD" -> Color(0xFF04201A)
        else -> MaterialTheme.colorScheme.background
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
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

                            val statusText = if (!chat?.typingStatus.isNullOrEmpty()) {
                                chat?.typingStatus!!
                            } else if (chat?.isGroup == true) {
                                "${chat?.memberCount} members"
                            } else {
                                "online • E2EE"
                            }

                            val subtitle = if (handleText.isNotEmpty()) "$handleText • $statusText" else statusText

                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = if (chat?.typingStatus.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else PulseGreen
                            )
                        }
                    }
                },
                actions = {
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
                                text = { Text("Wallpapers") },
                                onClick = {
                                    menuOpen = false
                                    selectedWallpaper = if (selectedWallpaper == "DEFAULT") "EMERALD" else "DEFAULT"
                                },
                                leadingIcon = { Icon(Icons.Outlined.Wallpaper, contentDescription = null) }
                            )
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(wallpaperBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages List
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

                    items(messages, key = { it.id }) { message ->
                        val isOutgoing = message.senderId == currentUserId
                        MessageBubbleRow(
                            message = message,
                            isOutgoing = isOutgoing,
                            currentUserId = currentUserId,
                            onLongClick = { showMsgOptionsForMsg = message },
                            onReactionClick = { showReactionDetailsForMsg = message }
                        )
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

                // Message Input Bar
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
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
                                placeholder = { Text("Message...", fontSize = 14.sp) },
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
                                    IconButton(onClick = { /* Emoji */ }) {
                                        Icon(Icons.Default.Face, contentDescription = "Emoji", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { showAttachmentSheet = true }) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = "Attach",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (inputText.isBlank()) {
                                            IconButton(onClick = { 
                                                viewModel.sendMessage(chatId, "Camera Photo", MessageType.IMAGE, "https://picsum.photos/seed/camera/800/600")
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
                        } else if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.sendMessage(chatId, inputText.trim())
                                    inputText = ""
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(PulseGreen, CircleShape)
                                    .testTag("send_message_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    isRecordingVoiceNote = true
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(VoiceMicAccent, CircleShape)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Note", tint = Color.White)
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
                                    viewModel.sendMessage(chatId, "Camera Photo", MessageType.IMAGE, "https://picsum.photos/seed/camera/800/600")
                                    showAttachmentSheet = false
                                }
                                AttachmentOptionItem("Gallery", Icons.Default.Image, Color(0xFF007BF5)) {
                                    viewModel.sendMessage(chatId, "Shared Image", MessageType.IMAGE, "https://picsum.photos/seed/sharedimg/800/600")
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
                                headlineContent = { Text(if (msg.isStarred) "Unstar Message" else "Star Message") },
                                leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    viewModel.toggleStarMessage(msg.id, !msg.isStarred)
                                    showMsgOptionsForMsg = null
                                }
                            )
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
                                    viewModel.deleteForMe(msg.id)
                                    showMsgOptionsForMsg = null
                                }
                            )
                            if (msg.senderId == currentUserId) {
                                ListItem(
                                    headlineContent = { Text("Delete for Everyone") },
                                    leadingContent = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red) },
                                    modifier = Modifier.clickable {
                                        viewModel.deleteForEveryone(msg.id)
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleRow(
    message: MessageEntity,
    isOutgoing: Boolean,
    currentUserId: String,
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
    val formattedTime = remember(message.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
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

@Composable
fun AttachmentOptionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
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
