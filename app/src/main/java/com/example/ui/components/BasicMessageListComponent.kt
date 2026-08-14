package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.MessageStatus
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a simple message item for the basic message list.
 */
data class MockMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = false,
    val status: String = MessageStatus.READ.name
)

/**
 * Default sample mock messages for preview and standalone usage.
 */
val defaultMockMessages = listOf(
    MockMessage(
        id = "mock_1",
        senderName = "Sarah Jenkins",
        content = "Hey! How is the project going?",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
        isOutgoing = false,
        status = MessageStatus.READ.name
    ),
    MockMessage(
        id = "mock_2",
        senderName = "You",
        content = "Going super smoothly! Just added real-time WebSocket connectivity and online presence indicators 🚀",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 12,
        isOutgoing = true,
        status = MessageStatus.READ.name
    ),
    MockMessage(
        id = "mock_3",
        senderName = "Sarah Jenkins",
        content = "Awesome! Does it support pinned messages and read receipts as well?",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 8,
        isOutgoing = false,
        status = MessageStatus.READ.name
    ),
    MockMessage(
        id = "mock_4",
        senderName = "You",
        content = "Yes, full double blue ticks, admin mode restrictions, and contact blocking are all built in.",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 2,
        isOutgoing = true,
        status = MessageStatus.DELIVERED.name
    )
)

/**
 * A reusable basic message list UI component using Compose that iterates over a list
 * of messages, featuring message bubbles, time formatting, read status ticks,
 * an input text field, an interactive send button, long-press delete confirmation dialogs,
 * an attachment button, and a typing status indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicMessageListComponent(
    modifier: Modifier = Modifier,
    initialMessages: List<MockMessage> = defaultMockMessages,
    isOtherUserTyping: Boolean = false,
    otherUserName: String = "Sarah Jenkins",
    onSendMessage: ((String) -> Unit)? = null
) {
    var messages by remember { mutableStateOf(initialMessages) }
    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<MockMessage?>(null) }
    var isSimulatingTyping by remember { mutableStateOf(isOtherUserTyping) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom whenever messages list grows or typing state changes
    LaunchedEffect(messages.size, isSimulatingTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size + (if (isSimulatingTyping) 0 else -1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Message list iterating over mock messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("basic_message_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MockMessageBubble(
                    message = message,
                    onLongClick = { messageToDelete = message }
                )
            }

            // Animated Typing status display component in the chat stream
            if (isSimulatingTyping) {
                item {
                    TypingStatusDisplay(
                        userName = otherUserName,
                        showAvatar = true,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }
        }

        // Delete Confirmation Dialog for long-pressed messages
        messageToDelete?.let { msg ->
            DeleteMessageConfirmationDialog(
                isOutgoing = msg.isOutgoing,
                onDismiss = { messageToDelete = null },
                onDeleteForMe = {
                    messages = messages.filter { it.id != msg.id }
                    messageToDelete = null
                },
                onDeleteForEveryone = if (msg.isOutgoing) {
                    {
                        messages = messages.filter { it.id != msg.id }
                        messageToDelete = null
                    }
                } else null
            )
        }

        // Attachment options modal sheet
        if (showAttachmentSheet) {
            ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Share Media & Attachments", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AttachmentOptionItem("Document", Icons.Default.Description, Color(0xFF5F66CD)) {
                            val newMsg = MockMessage(
                                senderName = "You",
                                content = "📄 Document: Architecture_Spec.pdf",
                                isOutgoing = true,
                                status = MessageStatus.SENT.name
                            )
                            messages = messages + newMsg
                            showAttachmentSheet = false
                        }
                        AttachmentOptionItem("Camera", Icons.Default.CameraAlt, Color(0xFFD3396D)) {
                            val newMsg = MockMessage(
                                senderName = "You",
                                content = "📷 Camera Photo (Shared)",
                                isOutgoing = true,
                                status = MessageStatus.SENT.name
                            )
                            messages = messages + newMsg
                            showAttachmentSheet = false
                        }
                        AttachmentOptionItem("Gallery", Icons.Default.Image, Color(0xFF007BF5)) {
                            val newMsg = MockMessage(
                                senderName = "You",
                                content = "🖼️ Gallery Image (Shared)",
                                isOutgoing = true,
                                status = MessageStatus.SENT.name
                            )
                            messages = messages + newMsg
                            showAttachmentSheet = false
                        }
                        AttachmentOptionItem("Audio", Icons.Default.Headphones, Color(0xFFF26522)) {
                            val newMsg = MockMessage(
                                senderName = "You",
                                content = "🎵 Voice Note (0:15)",
                                isOutgoing = true,
                                status = MessageStatus.SENT.name
                            )
                            messages = messages + newMsg
                            showAttachmentSheet = false
                        }
                    }
                }
            }
        }

        // Message Input Field and Send Button Bar
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input Text Field with Attachment and Emoji Icons
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Type a message...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("basic_message_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PulseGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    maxLines = 4,
                    leadingIcon = {
                        IconButton(onClick = { /* Emoji picker action */ }) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEmotions,
                                contentDescription = "Emoji Picker",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingIcon = {
                        // Attachment Icon for rich media sharing
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            modifier = Modifier.testTag("attachment_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attachment",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // Send Button
                val canSend = inputText.isNotBlank()
                IconButton(
                    onClick = {
                        if (canSend) {
                            val textToSend = inputText.trim()
                            val newMsg = MockMessage(
                                senderName = "You",
                                content = textToSend,
                                isOutgoing = true,
                                status = MessageStatus.SENT.name
                            )
                            messages = messages + newMsg
                            onSendMessage?.invoke(textToSend)
                            inputText = ""
                            scope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (canSend) PulseGreen else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("basic_message_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual message bubble for the mock message list with long-press support.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MockMessageBubble(
    message: MockMessage,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val isOutgoing = message.isOutgoing
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
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            modifier = Modifier.widthIn(max = 280.dp)
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
                    .then(
                        if (onLongClick != null) {
                            Modifier.combinedClickable(
                                onClick = { },
                                onLongClick = onLongClick
                            )
                        } else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    if (!isOutgoing) {
                        Text(
                            text = message.senderName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PulseGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Message Timestamp & Read Receipt Checkmarks
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
        }
    }
}
