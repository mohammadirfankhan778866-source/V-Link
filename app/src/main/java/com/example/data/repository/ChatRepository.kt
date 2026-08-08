package com.example.data.repository

import com.example.data.db.PulseDatabase
import com.example.data.models.*
import com.example.data.network.PulseWebSocketService
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class ReactionItem(
    val userId: String,
    val emoji: String,
    val userName: String
)

class ChatRepository(
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
        return database.messageDao().getMessagesForChat(chatId)
    }

    fun getChatById(chatId: String): Flow<ChatEntity?> {
        return database.chatDao().getChatById(chatId)
    }

    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        return database.messageDao().searchMessages(query)
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

    suspend fun toggleArchiveChat(chatId: String, isArchived: Boolean) {
        database.chatDao().updateArchived(chatId, isArchived)
    }

    suspend fun updateWallpaper(chatId: String, wallpaper: String) {
        database.chatDao().updateWallpaper(chatId, wallpaper)
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
        // Remove any fake seed data if present in DB
        database.userDao().deleteFakeUsers()
        database.chatDao().deleteFakeChats()
        database.messageDao().deleteFakeMessages()
        database.statusDao().deleteFakeStatuses()
        database.callDao().deleteFakeCalls()

        // Ensure Current User exists
        if (database.userDao().getCurrentUserOnce() == null) {
            val currentUser = UserEntity(
                id = "usr_google_irfan_9075",
                displayName = "Mohammad Irfan Khan",
                username = "@irfankhan",
                email = "mohammadirfankhan778866@gmail.com",
                profilePictureUrl = "https://picsum.photos/seed/irfan/300/300",
                bio = "Building high-performance Android systems ⚡",
                onlineStatus = "ONLINE",
                isCurrentUser = true
            )
            database.userDao().insertOrUpdateUser(currentUser)
        }

        // Seed channels if empty
        try {
            val channelCount = database.channelDao().getChannelById("channel_pulse_news")
            if (channelCount == null) {
                val seedChannels = listOf(
                    ChannelEntity(
                        id = "channel_pulse_news",
                        name = "Pulse Official News ⚡",
                        description = "Official broadcast channel for Pulse Chat updates, features, and system announcements.",
                        creatorId = "usr_system",
                        creatorName = "Pulse System",
                        avatarUrl = "https://picsum.photos/seed/pulsenews/300/300",
                        followerCount = 1250,
                        isFollowedByMe = true,
                        lastMessageText = "We have launched the Channels & Global Post feature! 🚀",
                        lastMessageTimestamp = System.currentTimeMillis() - 3600000
                    ),
                    ChannelEntity(
                        id = "channel_compose_art",
                        name = "Jetpack Compose Showcase 🎨",
                        description = "Daily doses of pure UI inspiration, glassmorphism templates, and Material 3 design systems.",
                        creatorId = "usr_elena",
                        creatorName = "Elena Rostova",
                        avatarUrl = "https://picsum.photos/seed/composeart/300/300",
                        followerCount = 4500,
                        isFollowedByMe = false,
                        lastMessageText = "Checkout this amazing Glassmorphic floating card animation!",
                        lastMessageTimestamp = System.currentTimeMillis() - 7200000
                    ),
                    ChannelEntity(
                        id = "channel_erlang_otp",
                        name = "Erlang/OTP Clustering 🌐",
                        description = "Deep dive into distributed systems, low-latency WebSockets, and BEAM performance optimization.",
                        creatorId = "usr_alex",
                        creatorName = "Alex Rivera",
                        avatarUrl = "https://picsum.photos/seed/erlangotp/300/300",
                        followerCount = 890,
                        isFollowedByMe = false,
                        lastMessageText = "Clustering setup completed across 4 geographic regions.",
                        lastMessageTimestamp = System.currentTimeMillis() - 14400000
                    )
                )
                database.channelDao().insertChannels(seedChannels)

                // Seed channel messages
                database.channelMessageDao().insertMessages(listOf(
                    ChannelMessageEntity(
                        id = "chan_msg_1",
                        channelId = "channel_pulse_news",
                        senderId = "usr_system",
                        senderName = "Pulse System",
                        content = "Welcome to the Pulse Official News Channel! Keep up to date with low-latency upgrades and OTP cluster releases.",
                        timestamp = System.currentTimeMillis() - 7200000,
                        mediaType = "TEXT"
                    ),
                    ChannelMessageEntity(
                        id = "chan_msg_2",
                        channelId = "channel_pulse_news",
                        senderId = "usr_system",
                        senderName = "Pulse System",
                        content = "We have launched the Channels & Global Post feature! 🚀 Now you can share photos, documents, and talk with unlimited followers.",
                        timestamp = System.currentTimeMillis() - 3600000,
                        mediaType = "TEXT"
                    )
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error seeding channels: ${e.message}")
        }

        // Seed posts if empty
        try {
            val postCount = database.postDao().getAllPosts()
            val existingSeedPost = database.postDao().getAllPosts()
            // Just insert the posts, they use REPLACE onConflict so it's totally safe to insert unconditionally
            val seedPosts = listOf(
                PostEntity(
                    id = "seed_post_irfan",
                    userId = "usr_google_irfan_9075",
                    userName = "Mohammad Irfan Khan",
                    userAvatar = "https://picsum.photos/seed/irfan/300/300",
                    content = "Just finalized the layout for the brand new GlassmorphicFloatingNavigationBar in Compose. Highly fluid, spring-bouncy, and matches the dynamic theme options perfectly! Let me know what you think of the new launcher logo too! 👇✨",
                    mediaUrl = "https://picsum.photos/seed/computercode/800/600",
                    mediaType = "IMAGE",
                    timestamp = System.currentTimeMillis() - 1800000,
                    likesCount = 42,
                    isLikedByMe = true
                ),
                PostEntity(
                    id = "seed_post_elena",
                    userId = "usr_elena",
                    userName = "Elena Rostova",
                    userAvatar = "https://picsum.photos/seed/elena/300/300",
                    content = "Enjoying a quiet, warm cup of coffee while reviewing Erlang OTP distributed cluster node logs. Low-latency is beautiful when done correctly! ☕📈 Check out my configuration file attached below.",
                    mediaUrl = "https://picsum.photos/seed/coffeedev/800/600",
                    mediaType = "IMAGE",
                    timestamp = System.currentTimeMillis() - 5400000,
                    likesCount = 28,
                    isLikedByMe = false
                ),
                PostEntity(
                    id = "seed_post_alex",
                    userId = "usr_alex",
                    userName = "Alex Rivera",
                    userAvatar = "https://picsum.photos/seed/alex/300/300",
                    content = "Building highly scalable Kotlin Multiplatform engines for local-first databases today. Decoupling storage logic from UI state makes Compose rendering exceptionally smooth. Check out the repository structure diagram! ⚡💻",
                    mediaUrl = "https://picsum.photos/seed/kotlinart/800/600",
                    mediaType = "IMAGE",
                    timestamp = System.currentTimeMillis() - 12000000,
                    likesCount = 56,
                    isLikedByMe = false
                )
            )
            database.postDao().insertPosts(seedPosts)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error seeding posts: ${e.message}")
        }
    }
}
