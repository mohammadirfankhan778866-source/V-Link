package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUserOnce(): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) OR LOWER(username) = LOWER('@' || :username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 0 ORDER BY displayName ASC")
    fun getAllContacts(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUserFlag()

    @Query("UPDATE users SET isCurrentUser = 1 WHERE id = :userId")
    suspend fun setCurrentUser(userId: String)

    @Query("UPDATE users SET onlineStatus = :status, lastSeenTimestamp = :lastSeen WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String, lastSeen: Long)

    @Query("DELETE FROM users WHERE id IN ('usr_sarah', 'usr_alex', 'usr_elena', 'usr_marcus')")
    suspend fun deleteFakeUsers()
}

@Dao
interface AccountCredentialDao {
    @Query("SELECT * FROM account_credentials WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getCredentialByEmail(email: String): AccountCredentialEntity?

    @Query("SELECT * FROM account_credentials WHERE LOWER(username) = LOWER(:username) OR LOWER(username) = LOWER('@' || :username) LIMIT 1")
    suspend fun getCredentialByUsername(username: String): AccountCredentialEntity?

    @Query("SELECT * FROM account_credentials WHERE id = :id LIMIT 1")
    suspend fun getCredentialById(id: String): AccountCredentialEntity?

    @Query("SELECT * FROM account_credentials")
    suspend fun getAllCredentials(): List<AccountCredentialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: AccountCredentialEntity)

    @Query("DELETE FROM account_credentials WHERE id = :id")
    suspend fun deleteCredential(id: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatById(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatByIdOnce(chatId: String): ChatEntity?

    @Query("SELECT DISTINCT c.* FROM chats c LEFT JOIN messages m ON c.id = m.chatId WHERE c.title LIKE '%' || :query || '%' OR c.username LIKE '%' || :query || '%' OR m.content LIKE '%' || :query || '%' ORDER BY c.lastMessageTimestamp DESC")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun updatePinned(chatId: String, isPinned: Boolean)

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun updateArchived(chatId: String, isArchived: Boolean)

    @Query("UPDATE chats SET typingStatus = :typingStatus WHERE id = :chatId")
    suspend fun updateTypingStatus(chatId: String, typingStatus: String)

    @Query("UPDATE chats SET wallpaperTheme = :wallpaperTheme WHERE id = :chatId")
    suspend fun updateWallpaper(chatId: String, wallpaperTheme: String)

    @Query("UPDATE chats SET adminsOnlyMode = :adminsOnly WHERE id = :chatId")
    suspend fun updateAdminsOnlyMode(chatId: String, adminsOnly: Boolean)

    @Query("UPDATE chats SET isBlocked = :isBlocked WHERE id = :chatId")
    suspend fun updateBlockedStatus(chatId: String, isBlocked: Boolean)

    @Query("UPDATE chats SET isOnline = :isOnline, lastSeenTimestamp = :lastSeen WHERE id = :chatId")
    suspend fun updateOnlineStatus(chatId: String, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun resetUnreadCount(chatId: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("DELETE FROM chats WHERE id IN ('chat_sarah', 'chat_tech_squad', 'chat_alex', 'chat_product_team')")
    suspend fun deleteFakeChats()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isDeletedForMe = 0 ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE (content LIKE '%' || :query || '%') AND isDeletedForMe = 0 ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE status = 'PENDING'")
    suspend fun getPendingMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun updateStarred(messageId: String, isStarred: Boolean)

    @Query("UPDATE messages SET isPinned = :isPinned WHERE id = :messageId")
    suspend fun updatePinned(messageId: String, isPinned: Boolean)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateReactions(messageId: String, reactions: String)

    @Query("UPDATE messages SET isDeletedForMe = 1 WHERE id = :messageId")
    suspend fun deleteForMe(messageId: String)

    @Query("UPDATE messages SET isDeletedForEveryone = 1, content = 'This message was deleted' WHERE id = :messageId")
    suspend fun deleteForEveryone(messageId: String)

    @Query("UPDATE messages SET content = :newContent, isEdited = 1, editedTimestamp = :timestamp WHERE id = :messageId")
    suspend fun editMessage(messageId: String, newContent: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE messages SET status = 'READ' WHERE chatId = :chatId AND senderId != :currentUserId AND status != 'READ'")
    suspend fun markIncomingMessagesAsRead(chatId: String, currentUserId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: String)

    @Query("DELETE FROM messages WHERE chatId IN ('chat_sarah', 'chat_tech_squad', 'chat_alex', 'chat_product_team')")
    suspend fun deleteFakeMessages()
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM status_stories ORDER BY timestamp DESC")
    fun getAllStatuses(): Flow<List<StatusStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusStoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<StatusStoryEntity>)

    @Query("UPDATE status_stories SET isViewed = 1 WHERE id = :statusId")
    suspend fun markStatusViewed(statusId: String)

    @Query("DELETE FROM status_stories WHERE userId IN ('usr_sarah', 'usr_alex', 'usr_elena', 'usr_marcus')")
    suspend fun deleteFakeStatuses()
}

@Dao
interface CallDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(call: CallLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(calls: List<CallLogEntity>)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLog(id: String)

    @Query("DELETE FROM call_logs WHERE contactId IN ('usr_sarah', 'usr_alex', 'usr_elena', 'usr_marcus', 'usr_contact_sara') OR contactName IN ('Sarah Jenkins', 'Alex Rivera', 'Elena Rostova', 'Marcus Vance')")
    suspend fun deleteFakeCalls()
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY lastMessageTimestamp DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels")
    suspend fun getAllChannelsOnce(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFollowedByMe = :followed, followerCount = followerCount + :countChange WHERE id = :channelId")
    suspend fun updateFollowState(channelId: String, followed: Boolean, countChange: Int)

    @Query("UPDATE channels SET lastMessageText = :text, lastMessageTimestamp = :timestamp WHERE id = :channelId")
    suspend fun updateLastMessage(channelId: String, text: String, timestamp: Long)

    @Query("UPDATE channels SET adminsOnlyMode = :adminsOnly WHERE id = :channelId")
    suspend fun updateChannelAdminsOnlyMode(channelId: String, adminsOnly: Boolean)

    @Query("UPDATE channels SET avatarUrl = :avatarUrl WHERE creatorId = :creatorId")
    suspend fun updateChannelAvatarsForCreator(creatorId: String, avatarUrl: String)

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)

    @Query("DELETE FROM channels WHERE id IN ('channel_pulse_news', 'channel_compose_art', 'channel_erlang_otp')")
    suspend fun deleteFakeChannels()
}

@Dao
interface ChannelMessageDao {
    @Query("SELECT * FROM channel_messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<ChannelMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChannelMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChannelMessageEntity>)

    @Query("DELETE FROM channel_messages WHERE channelId IN ('channel_pulse_news', 'channel_compose_art', 'channel_erlang_otp')")
    suspend fun deleteFakeChannelMessages()
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("UPDATE posts SET isLikedByMe = :liked, likesCount = likesCount + :countChange WHERE id = :postId")
    suspend fun updateLikeState(postId: String, liked: Boolean, countChange: Int)

    @Query("DELETE FROM posts WHERE id IN ('seed_post_irfan', 'seed_post_elena', 'seed_post_alex')")
    suspend fun deleteFakePosts()
}

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        StatusStoryEntity::class,
        CallLogEntity::class,
        ChannelEntity::class,
        ChannelMessageEntity::class,
        PostEntity::class,
        AccountCredentialEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun statusDao(): StatusDao
    abstract fun callDao(): CallDao
    abstract fun channelDao(): ChannelDao
    abstract fun channelMessageDao(): ChannelMessageDao
    abstract fun postDao(): PostDao
    abstract fun accountCredentialDao(): AccountCredentialDao

    companion object {
        @Volatile
        private var INSTANCE: PulseDatabase? = null

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chats ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE status_stories ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE call_logs ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE channels ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE posts ADD COLUMN isPremium INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chats ADD COLUMN adminIds TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE chats ADD COLUMN adminsOnlyMode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE channels ADD COLUMN adminsOnlyMode INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chats ADD COLUMN isOnline INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE chats ADD COLUMN lastSeenTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chats ADD COLUMN isBlocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN isEdited INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN editedTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS account_credentials (
                        id TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        username TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        passwordSalt TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        profilePictureUrl TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): PulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    "pulse_chat_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
