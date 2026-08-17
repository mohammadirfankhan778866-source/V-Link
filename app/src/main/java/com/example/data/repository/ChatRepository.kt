package com.example.data.repository

import android.content.Context
import com.example.data.db.PulseDatabase
import com.example.data.models.*
import com.example.data.network.PulseWebSocketService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class ReactionItem(
    val userId: String,
    val emoji: String,
    val userName: String
)

class ChatRepository(
    val context: Context,
    val database: PulseDatabase,
    val webSocketService: PulseWebSocketService
) {
    val allChats: Flow<List<ChatEntity>> = database.chatDao().getAllChats()
    val allContacts: Flow<List<UserEntity>> = database.userDao().getAllContacts()
    val currentUser: Flow<UserEntity?> = database.userDao().getCurrentUser()
    val allStatuses: Flow<List<StatusStoryEntity>> = database.statusDao().getAllStatuses()
    val allCalls: Flow<List<CallLogEntity>> = database.callDao().getAllCalls()
    val starredMessages: Flow<List<MessageEntity>> = database.messageDao().getStarredMessages()
    val allChannels: Flow<List<ChannelEntity>> = database.channelDao().getAllChannels()
    val allPosts: Flow<List<PostEntity>> = database.postDao().getAllPosts()

    fun getMessagesForChannel(channelId: String): Flow<List<ChannelMessageEntity>> {
        return database.channelMessageDao().getMessagesForChannel(channelId)
    }

    suspend fun createChannel(name: String, description: String, creatorId: String, creatorName: String, avatarUrl: String) {
        val channel = ChannelEntity(
            id = "channel_" + UUID.randomUUID().toString().take(6),
            name = name,
            description = description,
            creatorId = creatorId,
            creatorName = creatorName,
            avatarUrl = avatarUrl.ifBlank { "https://picsum.photos/seed/${name.length}/300/300" },
            followerCount = 1,
            isFollowedByMe = true,
            lastMessageText = "Channel created",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        database.channelDao().insertChannel(channel)
    }

    suspend fun toggleFollowChannel(channelId: String, isFollowing: Boolean) {
        database.channelDao().updateFollowState(channelId, !isFollowing, if (isFollowing) -1 else 1)
    }

    suspend fun sendChannelMessage(
        channelId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String,
        content: String,
        mediaUrl: String = "",
        mediaType: String = "TEXT",
        fileName: String = "",
        fileSize: String = ""
    ) {
        val msg = ChannelMessageEntity(
            id = "chan_msg_" + UUID.randomUUID().toString().take(6),
            channelId = channelId,
            senderId = senderId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            content = content,
            timestamp = System.currentTimeMillis(),
            mediaUrl = mediaUrl,
            fileName = fileName,
            fileSize = fileSize,
            mediaType = mediaType
        )
        database.channelMessageDao().insertMessage(msg)
        database.channelDao().updateLastMessage(channelId, if (mediaType != "TEXT") "📁 $content" else content, System.currentTimeMillis())
    }

    suspend fun createPost(
        userId: String,
        userName: String,
        userAvatar: String,
        content: String,
        mediaUrl: String = "",
        mediaType: String = "TEXT",
        fileExtension: String = "",
        fileSize: String = ""
    ) {
        val post = PostEntity(
            id = "post_" + UUID.randomUUID().toString().take(6),
            userId = userId,
            userName = userName,
            userAvatar = userAvatar,
            content = content,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            timestamp = System.currentTimeMillis(),
            likesCount = 0,
            isLikedByMe = false,
            fileExtension = fileExtension,
            fileSize = fileSize
        )
        database.postDao().insertPost(post)
    }

    suspend fun toggleLikePost(postId: String, isLiked: Boolean) {
        database.postDao().updateLikeState(postId, !isLiked, if (isLiked) -1 else 1)
    }

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return database.messageDao().getMessagesForChat(chatId).map { list ->
            list.map { msg ->
                if (com.example.util.E2EEncryptionManager.isEncrypted(msg.content)) {
                    msg.copy(content = com.example.util.E2EEncryptionManager.decrypt(msg.content, chatId))
                } else {
                    msg
                }
            }
        }
    }

    fun getChatById(chatId: String): Flow<ChatEntity?> {
        return database.chatDao().getChatById(chatId)
    }

    fun searchChats(query: String): Flow<List<ChatEntity>> {
        return database.chatDao().searchChats(query)
    }

    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        return database.messageDao().searchMessages(query).map { list ->
            list.map { msg ->
                if (com.example.util.E2EEncryptionManager.isEncrypted(msg.content)) {
                    msg.copy(content = com.example.util.E2EEncryptionManager.decrypt(msg.content, msg.chatId))
                } else {
                    msg
                }
            }
        }
    }

    suspend fun sendMessage(
        chatId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String = "",
        replyToId: String? = null,
        replyToName: String? = null,
        replyToContent: String? = null
    ) {
        webSocketService.sendMessage(
            chatId = chatId,
            content = content,
            type = type,
            mediaUrl = mediaUrl,
            replyToId = replyToId,
            replyToName = replyToName,
            replyToContent = replyToContent
        )
    }

    suspend fun togglePinChat(chatId: String, isPinned: Boolean) {
        database.chatDao().updatePinned(chatId, isPinned)
    }

    suspend fun setTypingStatus(chatId: String, typingStatus: String) {
        database.chatDao().updateTypingStatus(chatId, typingStatus)
    }

    suspend fun markChatAsRead(chatId: String, currentUserId: String = "usr_google_irfan_9075") {
        database.messageDao().markIncomingMessagesAsRead(chatId, currentUserId)
        database.chatDao().resetUnreadCount(chatId)
    }

    suspend fun editMessage(messageId: String, chatId: String, newContent: String) {
        val now = System.currentTimeMillis()
        database.messageDao().editMessage(messageId, newContent, now)
        val chat = database.chatDao().getChatByIdOnce(chatId)
        if (chat != null) {
            database.chatDao().insertOrUpdateChat(chat.copy(lastMessageText = newContent))
        }
    }

    suspend fun toggleAdminsOnlyMode(chatId: String, adminsOnly: Boolean) {
        database.chatDao().updateAdminsOnlyMode(chatId, adminsOnly)
    }

    suspend fun toggleArchiveChat(chatId: String, isArchived: Boolean) {
        database.chatDao().updateArchived(chatId, isArchived)
    }

    suspend fun updateWallpaper(chatId: String, wallpaper: String) {
        database.chatDao().updateWallpaper(chatId, wallpaper)
    }

    suspend fun toggleBlockUser(chatId: String, isBlocked: Boolean) {
        database.chatDao().updateBlockedStatus(chatId, isBlocked)
    }

    suspend fun togglePinMessage(messageId: String, isPinned: Boolean) {
        database.messageDao().updatePinned(messageId, isPinned)
    }

    suspend fun toggleStarMessage(messageId: String, isStarred: Boolean) {
        database.messageDao().updateStarred(messageId, isStarred)
    }

    suspend fun addReaction(messageId: String, reaction: String) {
        val currentUser = database.userDao().getCurrentUserOnce() ?: return
        val msg = database.messageDao().getMessageById(messageId) ?: return
        
        val currentReactionsStr = msg.reactions
        val reactionList = if (currentReactionsStr.isBlank()) {
            mutableListOf()
        } else {
            currentReactionsStr.split(",").mapNotNull { part ->
                val subParts = part.split(":")
                if (subParts.size >= 2) {
                    val rUserId = subParts[0]
                    val rEmoji = subParts[1]
                    val userName = if (subParts.size >= 3) subParts.drop(2).joinToString(":") else "User"
                    ReactionItem(rUserId, rEmoji, userName)
                } else null
            }.toMutableList()
        }

        // Check if current user already has a reaction
        val existingIndex = reactionList.indexOfFirst { it.userId == currentUser.id }
        if (existingIndex >= 0) {
            val existing = reactionList[existingIndex]
            if (existing.emoji == reaction) {
                // If the same emoji is clicked, remove it!
                reactionList.removeAt(existingIndex)
            } else {
                // If a different emoji is clicked, update/replace it!
                reactionList[existingIndex] = existing.copy(emoji = reaction)
            }
        } else {
            // New reaction!
            reactionList.add(ReactionItem(currentUser.id, reaction, currentUser.displayName))
        }

        val newReactionsStr = reactionList.joinToString(",") { "${it.userId}:${it.emoji}:${it.userName}" }
        database.messageDao().updateReactions(messageId, newReactionsStr)
    }

    suspend fun removeUserReaction(messageId: String, userId: String) {
        val msg = database.messageDao().getMessageById(messageId) ?: return
        val currentReactionsStr = msg.reactions
        if (currentReactionsStr.isBlank()) return
        
        val reactionList = currentReactionsStr.split(",").mapNotNull { part ->
            val subParts = part.split(":")
            if (subParts.size >= 2) {
                val rUserId = subParts[0]
                val rEmoji = subParts[1]
                val userName = if (subParts.size >= 3) subParts.drop(2).joinToString(":") else "User"
                ReactionItem(rUserId, rEmoji, userName)
            } else null
        }.filter { it.userId != userId }

        val newReactionsStr = reactionList.joinToString(",") { "${it.userId}:${it.emoji}:${it.userName}" }
        database.messageDao().updateReactions(messageId, newReactionsStr)
    }

    suspend fun deleteForMe(messageId: String) {
        database.messageDao().deleteForMe(messageId)
    }

    suspend fun clearChatMessages(chatId: String) {
        database.messageDao().clearChatMessages(chatId)
    }

    suspend fun deleteForEveryone(messageId: String) {
        database.messageDao().deleteForEveryone(messageId)
    }

    suspend fun createGroupChat(title: String, participantIds: List<String>): String {
        val chatId = "chat_group_" + UUID.randomUUID().toString().take(6)
        val newChat = ChatEntity(
            id = chatId,
            title = title,
            isGroup = true,
            avatarUrl = "https://picsum.photos/seed/$title/300/300",
            lastMessageText = "Group created by you",
            lastMessageTimestamp = System.currentTimeMillis(),
            memberCount = participantIds.size + 1
        )
        database.chatDao().insertOrUpdateChat(newChat)

        val sysMessage = MessageEntity(
            id = "msg_sys_" + UUID.randomUUID().toString().take(6),
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            content = "You created group \"$title\"",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.READ.name,
            type = MessageType.TEXT.name
        )
        database.messageDao().insertMessage(sysMessage)
        return chatId
    }

    suspend fun addMembersToGroup(chatId: String, newMemberNames: List<String>): Int {
        val chat = database.chatDao().getChatByIdOnce(chatId) ?: return 0
        val updatedMemberCount = chat.memberCount + newMemberNames.size
        database.chatDao().insertOrUpdateChat(chat.copy(memberCount = updatedMemberCount))

        val namesStr = newMemberNames.joinToString(", ")
        val sysMessage = MessageEntity(
            id = "msg_sys_" + UUID.randomUUID().toString().take(6),
            chatId = chatId,
            senderId = "system",
            senderName = "System",
            content = "You added $namesStr to the group",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.READ.name,
            type = MessageType.TEXT.name
        )
        database.messageDao().insertMessage(sysMessage)
        return updatedMemberCount
    }

    suspend fun postStatusStory(mediaUrl: String, caption: String) {
        val status = StatusStoryEntity(
            id = "status_" + UUID.randomUUID().toString().take(6),
            userId = "usr_google_irfan_9075",
            userName = "My Status",
            userAvatar = "https://picsum.photos/seed/irfan/300/300",
            mediaUrl = mediaUrl,
            caption = caption,
            timestamp = System.currentTimeMillis(),
            isViewed = true,
            isMine = true
        )
        database.statusDao().insertStatus(status)
    }

    suspend fun addCallLog(contactName: String, contactAvatar: String, callType: CallType, isIncoming: Boolean, isMissed: Boolean, duration: Int, contactUsername: String = "") {
        val call = CallLogEntity(
            id = "call_" + UUID.randomUUID().toString().take(6),
            contactId = "usr_contact_" + contactName.lowercase().take(4),
            contactName = contactName,
            contactUsername = contactUsername,
            contactAvatar = contactAvatar,
            callType = callType.name,
            isIncoming = isIncoming,
            isMissed = isMissed,
            timestamp = System.currentTimeMillis(),
            durationSeconds = duration
        )
        database.callDao().insertCallLog(call)
    }

    suspend fun markStatusViewed(statusId: String) {
        database.statusDao().markStatusViewed(statusId)
    }

    suspend fun populateSeedDataIfEmpty() {
        val prefs = context.getSharedPreferences("pulse_chat_seed_state", Context.MODE_PRIVATE)
        val isResetDone = prefs.getBoolean("vlink_account_reset_v1_done", false)
        if (!isResetDone) {
            // Delete all previous temporary accounts as requested
            database.clearAllTables()
            database.userDao().deleteFakeUsers()
            prefs.edit().putBoolean("vlink_account_reset_v1_done", true).apply()
        }

        // Clean up any remaining legacy mock data artifacts
        database.userDao().deleteFakeUsers()
        database.chatDao().deleteFakeChats()
        database.messageDao().deleteFakeMessages()
        database.statusDao().deleteFakeStatuses()
        database.callDao().deleteFakeCalls()
        database.channelDao().deleteFakeChannels()
        database.channelMessageDao().deleteFakeChannelMessages()
        database.postDao().deleteFakePosts()
    }
}
