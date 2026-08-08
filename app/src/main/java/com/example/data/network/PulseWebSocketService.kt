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
            val msgId = "msg_" + UUID.randomUUID().toString().take(8)
            val currentTime = System.currentTimeMillis()

            val message = MessageEntity(
                id = msgId,
                chatId = chatId,
                senderId = "usr_google_irfan_9075",
                senderName = "Mohammad Irfan Khan",
                senderAvatar = "https://picsum.photos/seed/irfan/300/300",
                content = content,
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
            val existingChat = database.chatDao().getChatByIdOnce(chatId)
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
            simulateServerEchoAndReply(chatId, content, type)
        }
    }

    private fun simulateServerEchoAndReply(chatId: String, userText: String, type: MessageType) {
        scope.launch {
            delay(600)
            val currentChat = database.chatDao().getChatByIdOnce(chatId)
            val contactName = currentChat?.title ?: "Contact"

            // Typing indicator from contact
            database.chatDao().updateTypingStatus(chatId, "$contactName is typing...")

            delay(1200)
            // Clear typing
            database.chatDao().updateTypingStatus(chatId, "")

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
                    "Received! Processing message via Pulse gateway. ⚡"
            }

            val replyMsgId = "msg_reply_" + UUID.randomUUID().toString().take(8)
            val replyTime = System.currentTimeMillis()

            val replyMsg = MessageEntity(
                id = replyMsgId,
                chatId = chatId,
                senderId = "usr_contact_" + chatId,
                senderName = contactName,
                senderAvatar = currentChat?.avatarUrl ?: "https://picsum.photos/seed/$chatId/300/300",
                content = responseText,
                timestamp = replyTime,
                status = MessageStatus.READ.name,
                type = MessageType.TEXT.name
            )

            database.messageDao().insertMessage(replyMsg)

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
