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

    fun sendMessage(chatId: String, content: String, type: MessageType = MessageType.TEXT, mediaUrl: String = "", replyToId: String? = null, replyToName: String? = null, replyToContent: String? = null) {
        scope.launch {
            val existingChat = database.chatDao().getChatByIdOnce(chatId)
            if (existingChat?.isBlocked == true) {
                // Prevent sending to blocked contact
                return@launch
            }

            val msgId = "msg_" + UUID.randomUUID().toString().take(8)
            val currentTime = System.currentTimeMillis()

            // Apply End-to-End Encryption
            val encryptedContent = com.example.util.E2EEncryptionManager.encrypt(content, chatId)

            val message = MessageEntity(
                id = msgId,
                chatId = chatId,
                senderId = "usr_google_irfan_9075",
                senderName = "Mohammad Irfan Khan",
                senderAvatar = "https://picsum.photos/seed/irfan/300/300",
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

            // Simulate server routing and response
            simulateServerEchoAndReply(chatId, content, type, msgId)
        }
    }

    private fun simulateServerEchoAndReply(chatId: String, userText: String, type: MessageType, userMsgId: String) {
        scope.launch {
            val currentChat = database.chatDao().getChatByIdOnce(chatId)
            if (currentChat?.isBlocked == true) {
                // Blocked contacts cannot reply or send messages
                return@launch
            }

            delay(400)
            database.messageDao().updateMessageStatus(userMsgId, MessageStatus.DELIVERED.name)
            delay(400)
            database.messageDao().updateMessageStatus(userMsgId, MessageStatus.READ.name)

            val contactName = currentChat?.title ?: "Contact"

            // Live Typing indicator from contact
            database.chatDao().updateTypingStatus(chatId, "$contactName is typing...")

            delay(1800)
            // Clear typing
            database.chatDao().updateTypingStatus(chatId, "")

            // Re-check blocking status after delay
            if (database.chatDao().getChatByIdOnce(chatId)?.isBlocked == true) {
                return@launch
            }

            // Generate realistic response based on chat
            val responseText = when {
                userText.lowercase().contains("hello") || userText.lowercase().contains("hi") ->
                    "Hey! 👋 How are you doing today?"
                userText.lowercase().contains("call") ->
                    "Sure! Let's get on a call soon."
                userText.lowercase().contains("image") || userText.lowercase().contains("photo") ->
                    "That looks great!"
                currentChat?.isGroup == true ->
                    "Got it! Thanks for updating the group."
                else ->
                    "Received! Protected with end-to-end encryption. 🔒⚡"
            }

            val replyMsgId = "msg_reply_" + UUID.randomUUID().toString().take(8)
            val replyTime = System.currentTimeMillis()

            val replyMsg = MessageEntity(
                id = replyMsgId,
                chatId = chatId,
                senderId = "usr_contact_" + chatId,
                senderName = contactName,
                senderAvatar = currentChat?.avatarUrl ?: "https://picsum.photos/seed/$chatId/300/300",
                content = com.example.util.E2EEncryptionManager.encrypt(responseText, chatId),
                timestamp = replyTime,
                status = MessageStatus.READ.name,
                type = MessageType.TEXT.name
            )

            database.messageDao().insertMessage(replyMsg)

            // Trigger heads-up notification for incoming message
            com.example.util.NotificationHelper.showNotification(
                context = com.example.PulseApplication.instance,
                title = contactName,
                message = responseText,
                channelId = com.example.PulseApplication.CHANNEL_MESSAGES
            )

            // Update chat last message & timestamp
            database.chatDao().getChatByIdOnce(chatId)?.let { chat ->
                val updatedChat = chat.copy(
                    lastMessageText = responseText,
                    lastMessageTimestamp = replyTime,
                    unreadCount = 0
                )
                database.chatDao().insertOrUpdateChat(updatedChat)
            }
        }
    }

    fun triggerIncomingCallSimulation(contactName: String, contactAvatar: String, isVideo: Boolean) {
        scope.launch {
            val chats = database.chatDao().getAllChatsOnce()
            val isCallerBlocked = chats.any { (it.title.equals(contactName, ignoreCase = true) || it.id.contains(contactName.lowercase())) && it.isBlocked }
            if (isCallerBlocked) {
                // Blocked contact call is dropped
                return@launch
            }

            val call = CallLogEntity(
                id = "call_" + UUID.randomUUID().toString().take(6),
                contactId = "usr_contact_" + contactName.lowercase().take(4),
                contactName = contactName,
                contactAvatar = contactAvatar.ifEmpty { "https://picsum.photos/seed/$contactName/300/300" },
                callType = if (isVideo) CallType.VIDEO.name else CallType.VOICE.name,
                isIncoming = true,
                isMissed = false,
                timestamp = System.currentTimeMillis(),
                durationSeconds = 0
            )
            _incomingCallSignal.value = call
        }
    }
}
