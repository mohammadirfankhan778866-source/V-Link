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

    @Query("SELECT * FROM users WHERE isCurrentUser = 0 ORDER BY displayName ASC")
    fun getAllContacts(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersOnce(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET onlineStatus = :status, lastSeenTimestamp = :lastSeen WHERE id = :userId")
    suspend fun updateUserStatus(userId: String, status: String, lastSeen: Long)

    @Query("DELETE FROM users WHERE id IN ('usr_sarah', 'usr_alex', 'usr_elena', 'usr_marcus')")
    suspend fun deleteFakeUsers()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatById(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatByIdOnce(chatId: String): ChatEntity?

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun updateStarred(messageId: String, isStarred: Boolean)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateReactions(messageId: String, reactions: String)

    @Query("UPDATE messages SET isDeletedForMe = 1 WHERE id = :messageId")
    suspend fun deleteForMe(messageId: String)

    @Query("UPDATE messages SET isDeletedForEveryone = 1, content = 'This message was deleted' WHERE id = :messageId")
    suspend fun deleteForEveryone(messageId: String)

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

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        StatusStoryEntity::class,
        CallLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun statusDao(): StatusDao
    abstract fun callDao(): CallDao

    companion object {
        @Volatile
        private var INSTANCE: PulseDatabase? = null

        fun getDatabase(context: Context): PulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulseDatabase::class.java,
                    "pulse_chat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
