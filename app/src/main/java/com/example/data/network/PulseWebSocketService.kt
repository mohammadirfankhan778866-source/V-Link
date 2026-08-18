package com.example.data.network

import com.example.data.db.PulseDatabase
import com.example.data.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

enum class WebSocketState {
    CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED
}

class PulseWebSocketService(private val database: PulseDatabase) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(WebSocketState.CONNECTED)
    val connectionState: StateFlow<WebSocketState> = _connectionState

    private val _incomingCallSignal = MutableStateFlow<CallLogEntity?>(null)
    val incomingCallSignal: StateFlow<CallLogEntity?> = _incomingCallSignal

    fun clearCallSignal() {
        _incomingCallSignal.value = null
    }

    fun sendMessage(
        chatId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String = "",
        replyToId: String? = null,
        replyToName: String? = null,
        replyToContent: String? = null
    ) {
        scope.launch {
            val existingChat = database.chatDao().getChatByIdOnce(chatId)
            if (existingChat?.isBlocked == true) {
                // Prevent sending to blocked contact
                return@launch
            }

            val currentUser = database.userDao().getCurrentUserOnce()
            val senderId = currentUser?.id ?: "usr_guest"
            val senderName = currentUser?.displayName ?: "User"
            val senderAvatar = currentUser?.profilePictureUrl ?: "https://picsum.photos/seed/$senderId/300/300"

            val msgId = "msg_" + UUID.randomUUID().toString().take(8)
            val currentTime = System.currentTimeMillis()

            // Apply End-to-End Encryption
            val encryptedContent = com.example.util.E2EEncryptionManager.encrypt(content, chatId)

            val message = MessageEntity(
                id = msgId,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                content = encryptedContent,
                timestamp = currentTime,
                status = MessageStatus.SENT.name,
                type = type.name,
                mediaUrl = mediaUrl,
                replyToMessageId = replyToId,
                replyToSenderName = replyToName,
                replyToContent = replyToContent
            )

            database.messageDao().insertMessage(message)

            // Update or create chat last message
            val displayTitle = existingChat?.title ?: "Chat"
            val updatedChat = (existingChat ?: ChatEntity(
                id = chatId,
                title = displayTitle,
                isGroup = false,
                avatarUrl = "https://picsum.photos/seed/$chatId/300/300",
                lastMessageText = "",
                lastMessageTimestamp = currentTime
            )).copy(
                lastMessageText = if (type == MessageType.TEXT) content else "📎 ${type.name.lowercase().replace("_", " ")}",
                lastMessageTimestamp = currentTime
            )
            database.chatDao().insertOrUpdateChat(updatedChat)

            delay(250)
            database.messageDao().updateMessageStatus(msgId, MessageStatus.DELIVERED.name)
        }
    }
}
