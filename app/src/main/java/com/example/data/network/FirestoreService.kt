package com.example.data.network

import android.util.Log
import com.example.data.models.MessageEntity
import com.example.data.models.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val TAG = "FirestoreService"

    // Safe lazy access to FirebaseFirestore instance
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore initialization error (app check or google services config missing): ${e.message}")
            null
        }
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth initialization error: ${e.message}")
            null
        }
    }

    /**
     * Checks if a username is unique and available across Firestore.
     */
    suspend fun isUsernameUnique(username: String): Boolean {
        val db = firestore ?: return true
        val cleanUsername = username.lowercase().removePrefix("@").trim()
        if (cleanUsername.isBlank()) return false

        return try {
            val querySnapshot = db.collection("usernames")
                .document(cleanUsername)
                .get()
                .await()
            !querySnapshot.exists()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore username check failed, falling back to local uniqueness check: ${e.message}")
            true
        }
    }

    /**
     * Checks if an email is already in use by another user in Firestore.
     */
    suspend fun isEmailUnique(email: String): Boolean {
        val db = firestore ?: return true
        val cleanEmail = email.lowercase().trim()
        if (cleanEmail.isBlank()) return false

        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()
            querySnapshot.isEmpty
        } catch (e: Exception) {
            Log.w(TAG, "Firestore email check failed: ${e.message}")
            true
        }
    }

    /**
     * Resolves a user by email address from Firestore.
     */
    suspend fun getUserByEmail(email: String): UserEntity? {
        val db = firestore ?: return null
        val cleanEmail = email.lowercase().trim()
        if (cleanEmail.isBlank()) return null

        return try {
            val querySnapshot = db.collection("users")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents[0]
                UserEntity(
                    id = doc.getString("id") ?: doc.id,
                    displayName = doc.getString("displayName") ?: "",
                    username = doc.getString("username") ?: "",
                    email = doc.getString("email") ?: cleanEmail,
                    profilePictureUrl = doc.getString("profilePictureUrl") ?: "",
                    bio = doc.getString("bio") ?: "",
                    emailVerified = doc.getBoolean("emailVerified") ?: false,
                    authProvider = doc.getString("authProvider") ?: "email",
                    isCurrentUser = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by email: ${e.message}")
            null
        }
    }

    /**
     * Resolves a unique username handle to its registered email address.
     */
    suspend fun getEmailByUsername(username: String): String? {
        val user = getUserByUsername(username)
        return user?.email
    }

    /**
     * Resolves a unique username handle to its UserEntity.
     */
    suspend fun getUserByUsername(username: String): UserEntity? {
        val db = firestore ?: return null
        val cleanUsername = username.lowercase().removePrefix("@").trim()
        if (cleanUsername.isBlank()) return null

        return try {
            val usernameSnapshot = db.collection("usernames").document(cleanUsername).get().await()
            if (usernameSnapshot.exists()) {
                val userId = usernameSnapshot.getString("userId") ?: return null
                getUser(userId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving user from username: ${e.message}")
            null
        }
    }

    /**
     * Saves a user account to Firestore and reserves their unique username.
     */
    suspend fun registerUser(user: UserEntity, passwordHash: String = "", passwordSalt: String = ""): Boolean {
        val db = firestore ?: return true
        val cleanUsername = user.username.lowercase().removePrefix("@").trim()

        return try {
            val usernameRef = db.collection("usernames").document(cleanUsername)
            val userRef = db.collection("users").document(user.id)

            db.runTransaction { transaction ->
                val usernameDoc = transaction.get(usernameRef)
                if (usernameDoc.exists() && usernameDoc.getString("userId") != user.id) {
                    throw IllegalStateException("Username @$cleanUsername is already taken by another user")
                }

                transaction.set(usernameRef, mapOf(
                    "userId" to user.id,
                    "username" to "@$cleanUsername",
                    "createdAt" to System.currentTimeMillis()
                ))

                val userData = mutableMapOf<String, Any>(
                    "id" to user.id,
                    "displayName" to user.displayName,
                    "username" to "@$cleanUsername",
                    "email" to user.email.lowercase().trim(),
                    "profilePictureUrl" to user.profilePictureUrl,
                    "bio" to user.bio,
                    "emailVerified" to user.emailVerified,
                    "authProvider" to user.authProvider,
                    "updatedAt" to System.currentTimeMillis()
                )
                if (passwordHash.isNotBlank()) {
                    userData["passwordHash"] = passwordHash
                    userData["passwordSalt"] = passwordSalt
                }

                transaction.set(userRef, userData)
            }.await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user in Firestore: ${e.message}")
            false
        }
    }

    suspend fun getPasswordCredentials(userId: String): Pair<String, String>? {
        val db = firestore ?: return null
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            if (snapshot.exists()) {
                val hash = snapshot.getString("passwordHash") ?: ""
                val salt = snapshot.getString("passwordSalt") ?: ""
                if (hash.isNotBlank()) Pair(hash, salt) else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUser(userId: String): UserEntity? {
        val db = firestore ?: return null
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            if (snapshot.exists()) {
                UserEntity(
                    id = snapshot.getString("id") ?: userId,
                    displayName = snapshot.getString("displayName") ?: "",
                    username = snapshot.getString("username") ?: "",
                    email = snapshot.getString("email") ?: "",
                    profilePictureUrl = snapshot.getString("profilePictureUrl") ?: "",
                    bio = snapshot.getString("bio") ?: "",
                    emailVerified = snapshot.getBoolean("emailVerified") ?: false,
                    authProvider = snapshot.getString("authProvider") ?: "",
                    isCurrentUser = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Registers or signs in a Google user in Firestore.
     */
    suspend fun registerGoogleUser(user: UserEntity): Boolean {
        val db = firestore ?: return true
        val cleanUsername = user.username.lowercase().removePrefix("@").trim()

        return try {
            val userRef = db.collection("users").document(user.id)
            userRef.set(mapOf(
                "id" to user.id,
                "displayName" to user.displayName,
                "username" to "@$cleanUsername",
                "email" to user.email,
                "profilePictureUrl" to user.profilePictureUrl,
                "bio" to user.bio,
                "emailVerified" to true,
                "authProvider" to "google.com",
                "updatedAt" to System.currentTimeMillis()
            ), com.google.firebase.firestore.SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Google user to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Saves a message document to Firestore under "chats/{chatId}/messages"
     */
    suspend fun saveMessage(chatId: String, message: MessageEntity) {
        val db = firestore ?: return
        try {
            val messageMap = mapOf(
                "id" to message.id,
                "chatId" to message.chatId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderAvatar" to message.senderAvatar,
                "content" to message.content,
                "timestamp" to message.timestamp,
                "status" to message.status,
                "type" to message.type,
                "mediaUrl" to message.mediaUrl,
                "reactions" to message.reactions,
                "isStarred" to message.isStarred
            )

            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(message.id)
                .set(messageMap)
                .await()

            // Update chat metadata in Firestore
            db.collection("chats")
                .document(chatId)
                .set(mapOf(
                    "id" to chatId,
                    "lastMessageText" to message.content,
                    "lastMessageTimestamp" to message.timestamp
                ), com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message to Firestore: ${e.message}")
        }
    }

    /**
     * Observes real-time messages for a specific chat from Firestore
     */
    fun observeChatMessages(chatId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        val listener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore message listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.data }
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Updates user profile fields (displayName, profilePictureUrl, bio) in Firestore document under "users/{userId}"
     */
    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        profilePictureUrl: String,
        bio: String
    ): Boolean {
        val db = firestore ?: return true
        return try {
            val userRef = db.collection("users").document(userId)
            val updates = mapOf(
                "displayName" to displayName,
                "profilePictureUrl" to profilePictureUrl,
                "bio" to bio,
                "updatedAt" to System.currentTimeMillis()
            )
            userRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile in Firestore: ${e.message}")
            false
        }
    }

    /**
     * Completely deletes a user's account and claims from Firestore & FirebaseAuth.
     */
    suspend fun deleteAccount(userId: String, username: String): Boolean {
        val db = firestore
        val cleanUsername = username.lowercase().removePrefix("@").trim()

        try {
            if (db != null) {
                // Delete user document
                db.collection("users").document(userId).delete().await()

                // Delete username claim document
                if (cleanUsername.isNotBlank()) {
                    db.collection("usernames").document(cleanUsername).delete().await()
                }
            }

            // Delete Firebase auth user
            auth?.currentUser?.delete()?.await()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting account in Firestore/Auth: ${e.message}")
            return true // Continue local deletion anyway
        }
    }
}
