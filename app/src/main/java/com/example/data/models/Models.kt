package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, VOICE_NOTE
}

enum class UserStatus {
    ONLINE, OFFLINE, TYPING, AWAY
}

enum class CallType {
    VOICE, VIDEO
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val username: String,
    val email: String,
    val profilePictureUrl: String,
    val bio: String,
    val onlineStatus: String = "ONLINE",
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val accountCreatedDate: String = "2026-01-15",
    val isCurrentUser: Boolean = false,
    val emailVerified: Boolean = false,
    val authProvider: String = "email",
    val chatProfilePictureUrl: String = "",
    val postProfilePictureUrl: String = "",
    val channelProfilePictureUrl: String = "",
    val channelAlias: String = "",
    val statusPrivacyMode: String = "PUBLIC",
    val statusPrivacyList: String = "",
    val isPremium: Boolean = false
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String, // Nick Name (e.g. "Mohammad Irfan Khan")
    val username: String = "", // Unique username handle (e.g. "@irfankhan")
    val isGroup: Boolean = false,
    val avatarUrl: String = "",
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val wallpaperTheme: String = "DEFAULT",
    val typingStatus: String = "", // e.g. "typing..." or ""
    val memberCount: Int = 1,
    val isPremium: Boolean = false,
    val adminIds: String = "",
    val adminsOnlyMode: Boolean = false,
    val isOnline: Boolean = true,
    val lastSeenTimestamp: Long = 0L,
    val isBlocked: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = MessageStatus.SENT.name,
    val type: String = MessageType.TEXT.name,
    val mediaUrl: String = "",
    val fileName: String = "",
    val fileSize: String = "",
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToContent: String? = null,
    val isStarred: Boolean = false,
    val isPinned: Boolean = false,
    val isDeletedForEveryone: Boolean = false,
    val isDeletedForMe: Boolean = false,
    val reactions: String = "", // e.g. "❤️,👍"
    val isPremium: Boolean = false,
    val isEdited: Boolean = false,
    val editedTimestamp: Long = 0L
)

@Entity(tableName = "status_stories")
data class StatusStoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val mediaUrl: String,
    val caption: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false,
    val isMine: Boolean = false,
    val isPremium: Boolean = false
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val contactName: String, // Nick Name (e.g. "Mohammad Irfan Khan")
    val contactUsername: String = "", // Unique handle (e.g. "@irfankhan")
    val contactAvatar: String,
    val callType: String = CallType.VOICE.name, // VOICE or VIDEO
    val isIncoming: Boolean = true,
    val isMissed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val isPremium: Boolean = false
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val creatorId: String,
    val creatorName: String,
    val avatarUrl: String,
    val followerCount: Int = 0,
    val isFollowedByMe: Boolean = false,
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val visibility: String = "PUBLIC", // PUBLIC, FRIENDS_ONLY
    val isPremium: Boolean = false,
    val adminsOnlyMode: Boolean = false
)

@Entity(tableName = "channel_messages")
data class ChannelMessageEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mediaUrl: String = "",
    val fileName: String = "",
    val fileSize: String = "",
    val mediaType: String = "TEXT" // TEXT, IMAGE, VIDEO, DOCUMENT
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val content: String,
    val mediaUrl: String = "",
    val mediaType: String = "TEXT", // TEXT, IMAGE, VIDEO, DOCUMENT
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val fileExtension: String = "",
    val fileSize: String = "",
    val visibility: String = "PUBLIC", // PUBLIC, FRIENDS_ONLY
    val isPremium: Boolean = false
)

@Entity(tableName = "account_credentials")
data class AccountCredentialEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val passwordHash: String,
    val passwordSalt: String,
    val displayName: String,
    val profilePictureUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class AdminAnalytics(
    val totalUsers: Int = 142850,
    val activeWebSocketConnections: Int = 89410,
    val totalMessagesToday: Int = 1249820,
    val serverCpuUsage: Float = 24.5f,
    val serverMemoryUsage: Float = 42.1f,
    val redisCacheHitRate: Float = 99.2f,
    val activeErlangNodes: Int = 12,
    val reportedMessages: Int = 14,
    val spamBlockedToday: Int = 312
)
