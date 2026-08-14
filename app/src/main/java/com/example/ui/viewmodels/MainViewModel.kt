package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PulseApplication
import com.example.data.models.*
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.data.network.FirestoreService
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

enum class NavigationTab {
    CHATS, UPDATES, POSTS, CHANNELS, CALLS
}

data class TempGoogleUser(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PulseApplication
    val repository = app.chatRepository
    val sessionManager = app.sessionManager
    val authRepository = app.authRepository
    val chatDraftDataStore = app.chatDraftDataStore
    val firestoreService = FirestoreService()

    val isLoggedIn = sessionManager.isLoggedIn
    val jwtToken = sessionManager.jwtToken
    val themeMode = sessionManager.themeMode
    val showExactTimestamps = sessionManager.showExactTimestamps

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = sessionManager.getCurrentUserId()
            if (userId != null && sessionManager.isLoggedIn.value) {
                repository.database.userDao().setCurrentUser(userId)
            }
        }
    }

    private val _tempGoogleUser = MutableStateFlow<TempGoogleUser?>(null)
    val tempGoogleUser: StateFlow<TempGoogleUser?> = _tempGoogleUser

    fun clearTempGoogleUser() {
        _tempGoogleUser.value = null
    }

    private val _currentTab = MutableStateFlow(NavigationTab.CHATS)
    val currentTab: StateFlow<NavigationTab> = _currentTab

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow("All") // All, Unread, Groups
    val selectedFilter: StateFlow<String> = _selectedFilter

    private val _activeCall = MutableStateFlow<CallLogEntity?>(null)
    val activeCall: StateFlow<CallLogEntity?> = _activeCall

    private val _isCallActiveScreenOpen = MutableStateFlow(false)
    val isCallActiveScreenOpen: StateFlow<Boolean> = _isCallActiveScreenOpen

    private val _isGroupCall = MutableStateFlow(false)
    val isGroupCall: StateFlow<Boolean> = _isGroupCall

    private val _groupParticipants = MutableStateFlow<List<CallParticipant>>(emptyList())
    val groupParticipants: StateFlow<List<CallParticipant>> = _groupParticipants

    private val _isWeakNetworkSimulated = MutableStateFlow(false)
    val isWeakNetworkSimulated: StateFlow<Boolean> = _isWeakNetworkSimulated

    private var groupCallJob: kotlinx.coroutines.Job? = null

    private val _isAdminDashboardOpen = MutableStateFlow(false)
    val isAdminDashboardOpen: StateFlow<Boolean> = _isAdminDashboardOpen

    private val _replyingToMessage = MutableStateFlow<MessageEntity?>(null)
    val replyingToMessage: StateFlow<MessageEntity?> = _replyingToMessage

    private val _activeStatusViewer = MutableStateFlow<StatusStoryEntity?>(null)
    val activeStatusViewer: StateFlow<StatusStoryEntity?> = _activeStatusViewer

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val chats: StateFlow<List<ChatEntity>> = combine(_searchQuery, _selectedFilter) { q, f -> Pair(q, f) }
        .flatMapLatest { (query, filter) ->
            val sourceFlow = if (query.isEmpty()) repository.allChats else repository.searchChats(query)
            sourceFlow.map { chatList ->
                chatList.filter { chat ->
                    val matchesFilter = when (filter) {
                        "Unread" -> chat.unreadCount > 0
                        "Groups" -> chat.isGroup
                        else -> !chat.isArchived
                    }
                    matchesFilter
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts = repository.allContacts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val currentUser = repository.currentUser.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val statuses = repository.allStatuses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val calls = repository.allCalls.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val starredMessages = repository.starredMessages.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val channels = repository.allChannels.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val posts = repository.allPosts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val connectionState: StateFlow<com.example.data.network.WebSocketState> = app.webSocketService.connectionState

    fun updateOnlineStatus(status: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.database.userDao().insertOrUpdateUser(
                user.copy(onlineStatus = status, lastSeenTimestamp = System.currentTimeMillis())
            )
        }
    }

    fun getMessagesForChannel(channelId: String): Flow<List<ChannelMessageEntity>> {
        return repository.getMessagesForChannel(channelId)
    }

    fun createChannel(name: String, description: String, avatarUrl: String, visibility: String = "PUBLIC") {
        viewModelScope.launch {
            val user = currentUser.value
            val creatorId = user?.id ?: "usr_google_irfan_9075"
            val creatorName = user?.channelAlias?.takeIf { it.isNotBlank() } ?: user?.displayName ?: "User"
            val finalAvatar = user?.channelProfilePictureUrl?.takeIf { it.isNotBlank() } ?: user?.profilePictureUrl ?: "https://picsum.photos/seed/irfan/300/300"
            
            val newChannel = ChannelEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                creatorId = creatorId,
                creatorName = creatorName,
                avatarUrl = avatarUrl.ifBlank { "https://picsum.photos/seed/${name.trim()}/300/300" },
                followerCount = 1,
                isFollowedByMe = true,
                visibility = visibility
            )
            repository.database.channelDao().insertChannel(newChannel)
        }
    }

    fun toggleFollowChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleFollowChannel(channel.id, channel.isFollowedByMe)
        }
    }

    fun sendChannelMessage(channelId: String, content: String, mediaUrl: String = "", mediaType: String = "TEXT", fileName: String = "", fileSize: String = "") {
        viewModelScope.launch {
            val user = currentUser.value
            val senderId = user?.id ?: "usr_google_irfan_9075"
            val senderName = user?.displayName ?: "Mohammad Irfan Khan"
            val senderAvatar = user?.profilePictureUrl ?: "https://picsum.photos/seed/irfan/300/300"
            repository.sendChannelMessage(channelId, senderId, senderName, senderAvatar, content, mediaUrl, mediaType, fileName, fileSize)
        }
    }

    fun getOrCreateChannelAndPost(channelName: String, content: String, mediaUrl: String, mediaType: String, fileName: String = "", fileSize: String = "", visibility: String = "PUBLIC") {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val creatorId = user?.id ?: "usr_google_irfan_9075"
            val creatorName = user?.displayName ?: "Mohammad Irfan Khan"
            val finalAvatar = user?.profilePictureUrl ?: "https://picsum.photos/seed/irfan/300/300"
            
            val existingChannels = repository.database.channelDao().getAllChannelsOnce()
            val existingChannel = existingChannels.find { it.name.equals(channelName.trim(), ignoreCase = true) && it.creatorId == creatorId }
            
            val channelId = if (existingChannel != null) {
                existingChannel.id
            } else {
                val newChannelId = "chan_" + java.util.UUID.randomUUID().toString().take(8)
                val newChannel = ChannelEntity(
                    id = newChannelId,
                    name = channelName.trim(),
                    description = "Official channel for $channelName uploads.",
                    creatorId = creatorId,
                    creatorName = creatorName,
                    avatarUrl = finalAvatar,
                    isFollowedByMe = true,
                    followerCount = 1,
                    lastMessageText = content.takeIf { it.isNotBlank() } ?: "Media shared",
                    lastMessageTimestamp = System.currentTimeMillis()
                )
                repository.database.channelDao().insertChannel(newChannel)
                newChannelId
            }
            
            repository.sendChannelMessage(
                channelId = channelId,
                senderId = creatorId,
                senderName = creatorName,
                senderAvatar = finalAvatar,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                fileName = fileName,
                fileSize = fileSize
            )
        }
    }

    fun createPost(content: String, mediaUrl: String = "", mediaType: String = "TEXT", fileExtension: String = "", fileSize: String = "", visibility: String = "PUBLIC") {
        viewModelScope.launch {
            val user = currentUser.value
            val userId = user?.id ?: "usr_google_irfan_9075"
            val userName = user?.displayName ?: "User"
            val userAvatar = user?.postProfilePictureUrl?.takeIf { it.isNotBlank() } ?: user?.profilePictureUrl ?: "https://picsum.photos/seed/irfan/300/300"
            
            val newPost = PostEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                fileExtension = fileExtension,
                fileSize = fileSize,
                visibility = visibility
            )
            repository.database.postDao().insertPost(newPost)
        }
    }

    fun toggleLikePost(post: PostEntity) {
        viewModelScope.launch {
            repository.toggleLikePost(post.id, post.isLikedByMe)
        }
    }

    private var channelSimulationJob: kotlinx.coroutines.Job? = null

    private fun startSimulatedChannelPosts() {
        channelSimulationJob?.cancel()
        channelSimulationJob = viewModelScope.launch(Dispatchers.Default) {
            // Wait 25 seconds after app startup before the first simulated post
            kotlinx.coroutines.delay(25000)
            
            val simulationMessages = listOf(
                "New OTP cluster node has been dynamically added to our Frankfurt zone! Latency is down to 4ms. ⚡" to "channel_erlang_otp",
                "Design tip: Ensure interactive elements are at least 48dp x 48dp for accessibility. 📱" to "channel_design_patterns",
                "Pulse Messenger version 2.4-alpha is now rolling out. Check out the brand new adaptive launcher icon! 🚀" to "channel_pulse_news",
                "Just launched a new server instance with zero downtime! BEAM scheduling is incredible. 🌐" to "channel_erlang_otp",
                "Remember to use Material Theme 3 colors to maintain proper light/dark contrast! 🎨" to "channel_design_patterns"
            )
            
            var index = 0
            while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                val (content, channelId) = simulationMessages[index]
                
                // Only post and notify if the user is following this channel!
                val currentChannels = channels.value
                val channel = currentChannels.find { it.id == channelId }
                if (channel != null && channel.isFollowedByMe) {
                    val msgId = "chan_sim_msg_" + java.util.UUID.randomUUID().toString().take(6)
                    val timestamp = System.currentTimeMillis()
                    
                    val simMsg = ChannelMessageEntity(
                        id = msgId,
                        channelId = channelId,
                        senderId = "usr_system",
                        senderName = channel.creatorName,
                        content = content,
                        timestamp = timestamp,
                        mediaType = "TEXT"
                    )
                    
                    // Insert the message to the database
                    repository.database.channelMessageDao().insertMessage(simMsg)
                    
                    // Update channel last message
                    repository.database.channelDao().updateLastMessage(channelId, content, timestamp)
                    
                    // Trigger native notification!
                    com.example.util.NotificationHelper.showNotification(
                        context = app,
                        title = "${channel.name} (${channel.creatorName})",
                        message = content,
                        channelId = com.example.PulseApplication.CHANNEL_MESSAGES
                    )
                }
                
                index = (index + 1) % simulationMessages.size
                // Wait another 45 seconds before the next simulated channel update
                kotlinx.coroutines.delay(45000)
            }
        }
    }

    init {
        // Collect incoming call signals from WebSocket service
        viewModelScope.launch {
            app.webSocketService.incomingCallSignal.collect { callSignal ->
                if (callSignal != null) {
                    _activeCall.value = callSignal
                    _isCallActiveScreenOpen.value = true
                }
            }
        }
        // Start simulation of posts by followed channels
        startSimulatedChannelPosts()
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun openChatDetail(chatId: String) {
        _activeChatId.value = chatId
    }

    fun startChatWithContact(contact: UserEntity) {
        val chatId = "chat_" + contact.id.replace("usr_", "")
        viewModelScope.launch {
            val existingChat = repository.database.chatDao().getChatByIdOnce(chatId)
            if (existingChat == null) {
                val newChat = ChatEntity(
                    id = chatId,
                    title = contact.displayName,
                    username = contact.username,
                    isGroup = false,
                    avatarUrl = contact.profilePictureUrl,
                    lastMessageText = "Tap to start chatting",
                    lastMessageTimestamp = System.currentTimeMillis()
                )
                repository.database.chatDao().insertOrUpdateChat(newChat)
            }
            _activeChatId.value = chatId
        }
    }

    fun addContact(name: String, username: String, bio: String) {
        viewModelScope.launch {
            val newUserId = "usr_" + java.util.UUID.randomUUID().toString().take(6)
            val cleanHandle = if (username.startsWith("@")) username else "@$username"
            val newUser = UserEntity(
                id = newUserId,
                displayName = name,
                username = cleanHandle,
                email = "${name.lowercase().replace(" ", "")}@pulse.chat",
                profilePictureUrl = "https://picsum.photos/seed/${name.lowercase()}/300/300",
                bio = bio.ifBlank { "Hey there! I am using V-Link." },
                onlineStatus = "ONLINE",
                isCurrentUser = false
            )
            repository.database.userDao().insertOrUpdateUser(newUser)
            startChatWithContact(newUser)
        }
    }

    fun closeChatDetail() {
        _activeChatId.value = null
        _replyingToMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setThemeMode(mode: AppThemeMode) {
        sessionManager.setThemeMode(mode)
    }

    fun setReplyingMessage(message: MessageEntity?) {
        _replyingToMessage.value = message
    }

    fun sendMessage(
        chatId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String = ""
    ) {
        val reply = _replyingToMessage.value
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                content = content,
                type = type,
                mediaUrl = mediaUrl,
                replyToId = reply?.id,
                replyToName = reply?.senderName,
                replyToContent = reply?.content
            )
            _replyingToMessage.value = null
            chatDraftDataStore.clearDraft(chatId)
        }
    }

    fun getChatDraft(chatId: String): Flow<String> {
        return chatDraftDataStore.getDraftFlow(chatId)
    }

    suspend fun getChatDraftOnce(chatId: String): String {
        return chatDraftDataStore.getDraft(chatId)
    }

    fun saveChatDraft(chatId: String, draft: String) {
        viewModelScope.launch {
            chatDraftDataStore.saveDraft(chatId, draft)
        }
    }

    fun clearChatDraft(chatId: String) {
        viewModelScope.launch {
            chatDraftDataStore.clearDraft(chatId)
        }
    }

    fun togglePinChat(chatId: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinChat(chatId, isPinned)
        }
    }

    fun toggleAdminsOnlyMode(chatId: String, adminsOnly: Boolean) {
        viewModelScope.launch {
            repository.toggleAdminsOnlyMode(chatId, adminsOnly)
        }
    }

    fun toggleArchiveChat(chatId: String, isArchived: Boolean) {
        viewModelScope.launch {
            repository.toggleArchiveChat(chatId, isArchived)
        }
    }

    fun updateWallpaper(chatId: String, wallpaper: String) {
        viewModelScope.launch {
            repository.updateWallpaper(chatId, wallpaper)
        }
    }

    fun toggleBlockUser(chatId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.toggleBlockUser(chatId, isBlocked)
        }
    }

    fun togglePinMessage(messageId: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinMessage(messageId, isPinned)
        }
    }

    fun toggleStarMessage(messageId: String, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStarMessage(messageId, isStarred)
        }
    }

    fun addReaction(messageId: String, reaction: String) {
        viewModelScope.launch {
            repository.addReaction(messageId, reaction)
        }
    }

    fun removeUserReaction(messageId: String, userId: String) {
        viewModelScope.launch {
            repository.removeUserReaction(messageId, userId)
        }
    }

    fun deleteForMe(messageId: String) {
        viewModelScope.launch {
            repository.deleteForMe(messageId)
        }
    }

    fun deleteForEveryone(messageId: String) {
        viewModelScope.launch {
            repository.deleteForEveryone(messageId)
        }
    }

    fun editMessage(messageId: String, chatId: String, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch {
            repository.editMessage(messageId, chatId, newContent.trim())
        }
    }

    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val currentUserId = user?.id ?: "usr_google_irfan_9075"
            repository.markChatAsRead(chatId, currentUserId)
        }
    }

    fun setUserTyping(chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            val typingText = if (isTyping) "typing..." else ""
            repository.setTypingStatus(chatId, typingText)
        }
    }

    fun toggleShowExactTimestamps(show: Boolean) {
        sessionManager.setShowExactTimestamps(show)
    }

    fun startCall(contactName: String, contactAvatar: String, isVideo: Boolean, contactUsername: String = "") {
        val handle = if (contactUsername.isBlank()) "@${contactName.lowercase().replace(" ", "_")}" else contactUsername
        val call = CallLogEntity(
            id = "call_" + System.currentTimeMillis(),
            contactId = "usr_contact_" + contactName.take(4),
            contactName = contactName,
            contactUsername = handle,
            contactAvatar = contactAvatar,
            callType = if (isVideo) CallType.VIDEO.name else CallType.VOICE.name,
            isIncoming = false,
            isMissed = false,
            timestamp = System.currentTimeMillis(),
            durationSeconds = 0
        )
        _isGroupCall.value = false
        _groupParticipants.value = emptyList()
        _activeCall.value = call
        _isCallActiveScreenOpen.value = true

        viewModelScope.launch {
            repository.addCallLog(
                contactName = contactName,
                contactAvatar = contactAvatar,
                callType = if (isVideo) CallType.VIDEO else CallType.VOICE,
                isIncoming = false,
                isMissed = false,
                duration = 45,
                contactUsername = handle
            )
        }
    }

    suspend fun startCallWithValidation(username: String, isVideo: Boolean): Pair<Boolean, String> {
        val cleanUsername = if (username.trim().startsWith("@")) username.trim().lowercase() else "@${username.trim().lowercase()}"
        if (cleanUsername.length <= 1) {
            return Pair(false, "Please enter a valid username handle")
        }

        return withContext(Dispatchers.IO) {
            val firestoreUser = firestoreService.getUserByUsername(cleanUsername)
            val userToCall = if (firestoreUser != null) {
                firestoreUser
            } else {
                val localContacts = repository.database.userDao().getAllUsersOnce()
                localContacts.firstOrNull { it.username.equals(cleanUsername, ignoreCase = true) }
            }

            if (userToCall == null) {
                return@withContext Pair(false, "User $cleanUsername does not exist on V-Link")
            }

            withContext(Dispatchers.Main) {
                startCall(
                    contactName = userToCall.displayName,
                    contactAvatar = userToCall.profilePictureUrl,
                    isVideo = isVideo,
                    contactUsername = userToCall.username
                )
            }
            return@withContext Pair(true, "Calling ${userToCall.displayName}")
        }
    }

    fun startGroupCall(groupTitle: String, groupAvatar: String, isVideo: Boolean) {
        val call = CallLogEntity(
            id = "gcall_" + System.currentTimeMillis(),
            contactId = "grp_" + groupTitle.lowercase().replace(" ", "_").take(8),
            contactName = groupTitle,
            contactUsername = "@vlink_group",
            contactAvatar = groupAvatar,
            callType = if (isVideo) CallType.VIDEO.name else CallType.VOICE.name,
            isIncoming = false,
            isMissed = false,
            timestamp = System.currentTimeMillis(),
            durationSeconds = 0
        )
        _isGroupCall.value = true
        _isWeakNetworkSimulated.value = false
        _activeCall.value = call
        _isCallActiveScreenOpen.value = true

        // Create default initial group call participants
        val initialParticipants = listOf(
            CallParticipant(
                id = "part_sarah",
                name = "Sarah Jenkins",
                avatarUrl = "https://picsum.photos/seed/sarah/150/150",
                connectionState = ParticipantConnectionState.RINGING,
                stats = WebRtcStats()
            ),
            CallParticipant(
                id = "part_david",
                name = "David Chen",
                avatarUrl = "https://picsum.photos/seed/david/150/150",
                connectionState = ParticipantConnectionState.RINGING,
                stats = WebRtcStats()
            ),
            CallParticipant(
                id = "part_emily",
                name = "Emily Rose",
                avatarUrl = "https://picsum.photos/seed/emily/150/150",
                connectionState = ParticipantConnectionState.RINGING,
                stats = WebRtcStats()
            )
        )
        _groupParticipants.value = initialParticipants

        // Save call log entry
        viewModelScope.launch {
            repository.addCallLog(
                contactName = groupTitle,
                contactAvatar = groupAvatar,
                callType = if (isVideo) CallType.VIDEO else CallType.VOICE,
                isIncoming = false,
                isMissed = false,
                duration = 120,
                contactUsername = "@group_call"
            )
        }

        // Run the dynamic group call multi-peer simulation
        startGroupCallSimulation()
    }

    private fun startGroupCallSimulation() {
        groupCallJob?.cancel()
        groupCallJob = viewModelScope.launch(Dispatchers.Default) {
            // Stage 1: Ringing states
            kotlinx.coroutines.delay(1200)
            _groupParticipants.update { list ->
                list.map {
                    if (it.id == "part_sarah") it.copy(connectionState = ParticipantConnectionState.CONNECTING) else it
                }
            }
            kotlinx.coroutines.delay(800)
            _groupParticipants.update { list ->
                list.map {
                    if (it.id == "part_sarah") it.copy(connectionState = ParticipantConnectionState.CONNECTED) else it
                }
            }

            // David Chen answers
            kotlinx.coroutines.delay(1000)
            _groupParticipants.update { list ->
                list.map {
                    if (it.id == "part_david") it.copy(connectionState = ParticipantConnectionState.CONNECTING) else it
                }
            }
            kotlinx.coroutines.delay(1000)
            _groupParticipants.update { list ->
                list.map {
                    if (it.id == "part_david") it.copy(connectionState = ParticipantConnectionState.CONNECTED) else it
                }
            }

            // Emily Rose declines or disconnects
            kotlinx.coroutines.delay(1200)
            _groupParticipants.update { list ->
                list.map {
                    if (it.id == "part_emily") it.copy(connectionState = ParticipantConnectionState.DECLINED) else it
                }
            }

            // Active speaker and real-time streaming simulation loop
            var speakerIndex = 0
            val activeSpeakerIds = listOf("part_sarah", "part_david", "self")
            val random = java.util.Random()

            while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                kotlinx.coroutines.delay(2500)
                speakerIndex = (speakerIndex + 1) % activeSpeakerIds.size
                val currentSpeakerId = activeSpeakerIds[speakerIndex]

                _groupParticipants.update { list ->
                    list.map { p ->
                        if (p.connectionState == ParticipantConnectionState.CONNECTED) {
                            val isSpeaking = p.id == currentSpeakerId
                            val audioLevel = if (isSpeaking) (0.35f + random.nextFloat() * 0.55f) else 0.0f
                            
                            val isWeak = _isWeakNetworkSimulated.value
                            val baseBitrate = if (isWeak) random.nextInt(60) + 40 else random.nextInt(250) + 750
                            val baseLoss = if (isWeak) 7.5f + random.nextFloat() * 3f else random.nextFloat() * 0.2f
                            val jitter = random.nextInt(10) - 5
                            val updatedStats = p.stats.copy(
                                bitrateKbps = baseBitrate,
                                packetLossPct = baseLoss,
                                latencyMs = (if (isWeak) 190 else 40) + jitter,
                                resolution = if (isWeak) "360p @ 12fps" else "1080p @ 30fps"
                            )

                            p.copy(
                                isSpeaking = isSpeaking,
                                audioLevel = audioLevel,
                                stats = updatedStats
                            )
                        } else {
                            p
                        }
                    }
                }
            }
        }
    }

    fun inviteParticipantToGroupCall(name: String, avatarUrl: String) {
        val newId = "part_" + name.lowercase().replace(" ", "_")
        if (_groupParticipants.value.any { it.id == newId }) return

        val newParticipant = CallParticipant(
            id = newId,
            name = name,
            avatarUrl = avatarUrl,
            connectionState = ParticipantConnectionState.RINGING,
            stats = WebRtcStats()
        )
        _groupParticipants.update { it + newParticipant }

        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            _groupParticipants.update { list ->
                list.map {
                    if (it.id == newId) it.copy(connectionState = ParticipantConnectionState.CONNECTING) else it
                }
            }
            kotlinx.coroutines.delay(1200)
            _groupParticipants.update { list ->
                list.map {
                    if (it.id == newId) it.copy(connectionState = ParticipantConnectionState.CONNECTED) else it
                }
            }
        }
    }

    fun disconnectParticipant(id: String) {
        _groupParticipants.update { list ->
            list.map {
                if (it.id == id) it.copy(connectionState = ParticipantConnectionState.DISCONNECTED, isSpeaking = false, audioLevel = 0f) else it
            }
        }
    }

    fun muteParticipantLocally(id: String, isMuted: Boolean) {
        _groupParticipants.update { list ->
            list.map {
                if (it.id == id) it.copy(isMutedByMe = isMuted) else it
            }
        }
    }

    fun adjustParticipantVolume(id: String, volume: Float) {
        _groupParticipants.update { list ->
            list.map {
                if (it.id == id) it.copy(volume = volume) else it
            }
        }
    }

    fun toggleWeakNetworkSimulation() {
        _isWeakNetworkSimulated.value = !_isWeakNetworkSimulated.value
    }

    
    suspend fun performLoginBack(usernameOrEmail: String, password: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            val input = usernameOrEmail.trim()
            if (input.isEmpty() || password.isEmpty()) {
                return@withContext Pair(false, "Please enter your username/email and password.")
            }
            val isEmail = input.contains("@") && !input.startsWith("@")
            val cleanUsername = input.removePrefix("@").lowercase()

            try {
                // 1. Look up account in local DB credentials
                val localCred = if (isEmail) {
                    repository.database.accountCredentialDao().getCredentialByEmail(input)
                } else {
                    repository.database.accountCredentialDao().getCredentialByUsername(cleanUsername)
                }

                // 2. Look up account in Firestore if not found locally
                val remoteUser = if (localCred == null) {
                    if (isEmail) firestoreService.getUserByEmail(input) else firestoreService.getUserByUsername(cleanUsername)
                } else null

                val targetEmail = localCred?.email ?: remoteUser?.email ?: if (isEmail) input else null
                val targetUserId = localCred?.id ?: remoteUser?.id

                // 3. Try Firebase Auth sign in if email is known
                var firebaseSuccess = false
                var firebaseUser: com.google.firebase.auth.FirebaseUser? = null
                if (targetEmail != null) {
                    val authResult = authRepository.signInWithEmailAndPassword(targetEmail, password)
                    if (authResult.isSuccess && authResult.getOrNull()?.user != null) {
                        firebaseSuccess = true
                        firebaseUser = authResult.getOrNull()!!.user
                    }
                }

                // 4. Verify password against local SHA-256 hash or Firestore hash
                var localVerified = false
                if (localCred != null) {
                    localVerified = com.example.util.AuthCryptoUtils.verifyPassword(
                        password = password,
                        salt = localCred.passwordSalt,
                        expectedHash = localCred.passwordHash
                    )
                }

                var remoteVerified = false
                if (!firebaseSuccess && !localVerified && targetUserId != null) {
                    val remoteCred = firestoreService.getPasswordCredentials(targetUserId)
                    if (remoteCred != null) {
                        remoteVerified = com.example.util.AuthCryptoUtils.verifyPassword(
                            password = password,
                            salt = remoteCred.second,
                            expectedHash = remoteCred.first
                        )
                    }
                }

                if (firebaseSuccess || localVerified || remoteVerified) {
                    val userId = firebaseUser?.uid ?: targetUserId ?: "usr_${cleanUsername}"
                    
                    // Fetch or reconstruct user entity
                    var userEntity = repository.database.userDao().getUserById(userId)
                        ?: (if (targetUserId != null) firestoreService.getUser(targetUserId) else null)

                    if (userEntity == null) {
                        val cleanHandle = "@" + (if (isEmail) input.substringBefore("@") else cleanUsername)
                        userEntity = UserEntity(
                            id = userId,
                            displayName = firebaseUser?.displayName ?: localCred?.displayName ?: remoteUser?.displayName ?: input.substringBefore("@"),
                            username = cleanHandle,
                            email = targetEmail ?: input,
                            profilePictureUrl = firebaseUser?.photoUrl?.toString() ?: localCred?.profilePictureUrl ?: "https://picsum.photos/seed/$userId/300/300",
                            bio = "Connecting via V-Link ⚡",
                            onlineStatus = "ONLINE",
                            isCurrentUser = true,
                            emailVerified = true,
                            authProvider = "email"
                        )
                        firestoreService.registerUser(userEntity)
                    }

                    // Save credentials locally if not present
                    if (localCred == null) {
                        val salt = com.example.util.AuthCryptoUtils.generateSalt()
                        val hash = com.example.util.AuthCryptoUtils.hashPassword(password, salt)
                        repository.database.accountCredentialDao().insertCredential(
                            AccountCredentialEntity(
                                id = userEntity.id,
                                email = userEntity.email,
                                username = userEntity.username,
                                passwordHash = hash,
                                passwordSalt = salt,
                                displayName = userEntity.displayName,
                                profilePictureUrl = userEntity.profilePictureUrl
                            )
                        )
                    }

                    repository.database.userDao().clearCurrentUserFlag()
                    repository.database.userDao().insertOrUpdateUser(userEntity.copy(isCurrentUser = true))
                    sessionManager.saveCustomUserSession("vlink_jwt_${System.currentTimeMillis()}", userEntity)
                    return@withContext Pair(true, "Login successful")
                } else {
                    return@withContext Pair(false, "Incorrect username/email or password. Please verify your credentials.")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Login error: ${e.message}")
                return@withContext Pair(false, "Login failed: ${e.message ?: "Please check your network and credentials"}")
            }
        }
    }

    suspend fun sendPasswordResetLink(usernameOrEmail: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            val input = usernameOrEmail.trim()
            if (input.isEmpty()) {
                return@withContext Pair(false, "Please enter your registered email address or @username.")
            }
            val isEmail = input.contains("@") && !input.startsWith("@")
            val cleanUsername = input.removePrefix("@").lowercase()

            try {
                // 1. Resolve email address
                var targetEmail: String? = null
                if (isEmail) {
                    targetEmail = input.lowercase()
                } else {
                    val localCred = repository.database.accountCredentialDao().getCredentialByUsername(cleanUsername)
                    val localUser = repository.database.userDao().getUserByUsername(cleanUsername)
                    val remoteUser = firestoreService.getUserByUsername(cleanUsername)
                    targetEmail = localCred?.email ?: localUser?.email ?: remoteUser?.email ?: firestoreService.getEmailByUsername(cleanUsername)
                }

                if (targetEmail.isNullOrBlank()) {
                    return@withContext Pair(false, "Could not find any account associated with '$input'. Please check the spelling or register a new account.")
                }

                // 2. Request Firebase Auth Password Reset Email
                val result = authRepository.sendPasswordResetEmail(targetEmail)
                if (result.isSuccess) {
                    return@withContext Pair(true, "Password reset instructions have been sent to $targetEmail. Please check your inbox and spam folder.")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message
                    if (errorMsg != null && errorMsg.contains("no user record", ignoreCase = true)) {
                        return@withContext Pair(false, "No Firebase record found for $targetEmail. You can create a new account or sign in with your password.")
                    }
                    return@withContext Pair(true, "Password reset instructions have been sent to $targetEmail.")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Reset password request error: ${e.message}")
                return@withContext Pair(false, "Failed to send reset link: ${e.message ?: "Please try again later"}")
            }
        }
    }

    fun endCall() {
        _isCallActiveScreenOpen.value = false
        _activeCall.value = null
        _isGroupCall.value = false
        _groupParticipants.value = emptyList()
        groupCallJob?.cancel()
        groupCallJob = null
        app.webSocketService.clearCallSignal()
    }

    fun openStatusViewer(status: StatusStoryEntity) {
        _activeStatusViewer.value = status
        viewModelScope.launch {
            repository.markStatusViewed(status.id)
        }
    }

    fun closeStatusViewer() {
        _activeStatusViewer.value = null
    }

    fun postStatus(mediaUrl: String, caption: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val userId = user?.id ?: "usr_google_irfan_9075"
            val userName = user?.displayName ?: "User"
            val userAvatar = user?.chatProfilePictureUrl?.takeIf { it.isNotBlank() } ?: user?.profilePictureUrl ?: "https://picsum.photos/seed/irfan/300/300"
            
            val status = StatusStoryEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                mediaUrl = mediaUrl,
                caption = caption,
                isMine = true
            )
            repository.database.statusDao().insertStatus(status)
        }
    }

    fun createGroupChat(title: String, participantIds: List<String>) {
        viewModelScope.launch {
            val newChatId = repository.createGroupChat(title, participantIds)
            openChatDetail(newChatId)
        }
    }

    fun toggleAdminDashboard(open: Boolean) {
        _isAdminDashboardOpen.value = open
    }

    fun performGoogleLogin(
        context: android.content.Context,
        email: String = "mohammadirfankhan778866@gmail.com",
        displayName: String = "Mohammad Irfan Khan",
        avatarUrl: String = "https://picsum.photos/seed/irfan/300/300"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var fbUid: String? = null
            var fbEmail: String? = null
            var fbDisplayName: String? = null
            var fbPhotoUrl: String? = null

            try {
                val result = authRepository.signInWithGoogle(context)
                if (result != null && result.user != null) {
                    val fbUser = result.user!!
                    fbUid = fbUser.uid
                    fbEmail = fbUser.email
                    fbDisplayName = fbUser.displayName
                    fbPhotoUrl = fbUser.photoUrl?.toString()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Real Google Sign In failed, using Quick Sign In fallback: ${e.message}")
            }

            // Fallback to quick/mock Google Sign-In if real one returned null (e.g. in emulator or missing services)
            if (fbUid == null) {
                fbUid = "usr_google_mock_" + email.substringBefore("@").replace(".", "_")
                fbEmail = email
                fbDisplayName = displayName
                fbPhotoUrl = avatarUrl
            }

            // Fetch existing user profile from Firestore
            var userEntity = firestoreService.getUser(fbUid)
            
            if (userEntity == null) {
                // If it is a new user, trigger the custom Nick Name & Handle setup screen!
                _tempGoogleUser.value = TempGoogleUser(
                    id = fbUid,
                    email = fbEmail ?: email,
                    displayName = fbDisplayName ?: displayName,
                    avatarUrl = fbPhotoUrl ?: avatarUrl
                )
            } else {
                // User already exists, log in directly!
                repository.database.userDao().insertOrUpdateUser(userEntity.copy(isCurrentUser = true))
                sessionManager.saveCustomUserSession("vlink_jwt_${System.currentTimeMillis()}", userEntity)
            }
        }
    }

    suspend fun performRealGoogleLogin(context: android.content.Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = authRepository.signInWithGoogle(context)
                if (result != null && result.user != null) {
                    val fbUser = result.user!!
                    val fbUid = fbUser.uid
                    val fbEmail = fbUser.email ?: ""
                    val fbDisplayName = fbUser.displayName ?: fbEmail.substringBefore("@")
                    val fbPhotoUrl = fbUser.photoUrl?.toString() ?: "https://picsum.photos/seed/${fbUid}/300/300"

                    var userEntity = firestoreService.getUser(fbUid)
                    if (userEntity == null) {
                        _tempGoogleUser.value = TempGoogleUser(
                            id = fbUid,
                            email = fbEmail,
                            displayName = fbDisplayName,
                            avatarUrl = fbPhotoUrl
                        )
                    } else {
                        repository.database.userDao().insertOrUpdateUser(userEntity.copy(isCurrentUser = true))
                        sessionManager.saveCustomUserSession("vlink_jwt_${System.currentTimeMillis()}", userEntity)
                    }
                    return@withContext true
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Real native Google Sign In failed: ${e.message}")
            }
            return@withContext false
        }
    }

    fun performQuickGoogleSignIn(email: String, displayName: String, avatarUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val fbUid = "usr_google_mock_" + email.substringBefore("@").replace(".", "_")
            var userEntity = firestoreService.getUser(fbUid)
            if (userEntity == null) {
                _tempGoogleUser.value = TempGoogleUser(
                    id = fbUid,
                    email = email,
                    displayName = displayName,
                    avatarUrl = avatarUrl
                )
            } else {
                repository.database.userDao().insertOrUpdateUser(userEntity.copy(isCurrentUser = true))
                sessionManager.saveCustomUserSession("vlink_jwt_${System.currentTimeMillis()}", userEntity)
            }
        }
    }

    fun completeGoogleRegistration(displayName: String, username: String) {
        val temp = _tempGoogleUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val cleanUsername = if (username.startsWith("@")) username else "@$username"
            val userEntity = UserEntity(
                id = temp.id,
                displayName = displayName,
                username = cleanUsername,
                email = temp.email,
                profilePictureUrl = temp.avatarUrl,
                bio = "Connecting via V-Link ⚡",
                onlineStatus = "ONLINE",
                isCurrentUser = true,
                emailVerified = true,
                authProvider = "google.com"
            )
            // Transactionally registers user and reserves their unique username
            val success = firestoreService.registerUser(userEntity)
            if (success) {
                repository.database.userDao().insertOrUpdateUser(userEntity.copy(isCurrentUser = true))
                sessionManager.saveCustomUserSession("vlink_jwt_${System.currentTimeMillis()}", userEntity)
                _tempGoogleUser.value = null
            }
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            firestoreService.isUsernameUnique(username)
        }
    }

    suspend fun registerUserWithUniqueUsername(
        displayName: String,
        username: String,
        email: String,
        password: String
    ): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            val cleanUsername = if (username.startsWith("@")) username.lowercase().trim() else "@${username.lowercase().trim()}"
            val cleanEmail = email.trim().lowercase()

            // 1. Basic validation
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                return@withContext Pair(false, "Please enter a valid email address (e.g. name@domain.com).")
            }
            if (password.length < 6) {
                return@withContext Pair(false, "Password must be at least 6 characters.")
            }
            if (displayName.trim().isEmpty()) {
                return@withContext Pair(false, "Please enter your Nick Name.")
            }

            try {
                // 2. Check username availability locally and on Firestore
                val existingLocalUser = repository.database.userDao().getUserByUsername(cleanUsername)
                val existingLocalCred = repository.database.accountCredentialDao().getCredentialByUsername(cleanUsername)
                if (existingLocalUser != null || existingLocalCred != null) {
                    return@withContext Pair(false, "Username $cleanUsername is already taken on this device. Please choose another.")
                }

                val isUsernameFree = firestoreService.isUsernameUnique(cleanUsername)
                if (!isUsernameFree) {
                    return@withContext Pair(false, "Username $cleanUsername is already taken globally. Please choose another.")
                }

                // 3. Check email uniqueness locally and on Firestore
                val existingEmailUser = repository.database.userDao().getUserByEmail(cleanEmail)
                val existingEmailCred = repository.database.accountCredentialDao().getCredentialByEmail(cleanEmail)
                if (existingEmailUser != null || existingEmailCred != null) {
                    return@withContext Pair(false, "An account with email $cleanEmail already exists. Please Log In using your email and password.")
                }

                val isEmailFree = firestoreService.isEmailUnique(cleanEmail)
                if (!isEmailFree) {
                    return@withContext Pair(false, "An account with email $cleanEmail is already registered. Please Log In instead.")
                }

                // 4. Register with Firebase Authentication
                val authResult = authRepository.registerWithEmailAndPassword(cleanEmail, password)
                var userId: String
                val registeredUser = authResult.getOrNull()?.user
                if (authResult.isSuccess && registeredUser != null) {
                    userId = registeredUser.uid
                } else {
                    val ex = authResult.exceptionOrNull()
                    if (ex is com.google.firebase.auth.FirebaseAuthUserCollisionException || ex?.message?.contains("already in use", ignoreCase = true) == true) {
                        return@withContext Pair(false, "This email is already in use by another account. Please Log In with your password or use your own email.")
                    }
                    // Offline / local unique ID generation
                    userId = "usr_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
                }

                val salt = com.example.util.AuthCryptoUtils.generateSalt()
                val passwordHash = com.example.util.AuthCryptoUtils.hashPassword(password, salt)

                val user = UserEntity(
                    id = userId,
                    displayName = displayName.trim(),
                    username = cleanUsername,
                    email = cleanEmail,
                    profilePictureUrl = "https://picsum.photos/seed/${cleanUsername.removePrefix("@")}/300/300",
                    bio = "Connecting via V-Link ⚡",
                    onlineStatus = "ONLINE",
                    isCurrentUser = true,
                    emailVerified = true,
                    authProvider = "email"
                )

                // Save to Firestore
                try {
                    firestoreService.registerUser(user, passwordHash, salt)
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Firestore register warning: ${e.message}")
                }

                // Save credentials in Room DB
                repository.database.accountCredentialDao().insertCredential(
                    AccountCredentialEntity(
                        id = userId,
                        email = cleanEmail,
                        username = cleanUsername,
                        passwordHash = passwordHash,
                        passwordSalt = salt,
                        displayName = user.displayName,
                        profilePictureUrl = user.profilePictureUrl
                    )
                )

                // Set as active user
                repository.database.userDao().clearCurrentUserFlag()
                repository.database.userDao().insertOrUpdateUser(user)

                // Save session
                sessionManager.saveCustomUserSession("vlink_jwt_${System.currentTimeMillis()}", user)
                return@withContext Pair(true, "Account created successfully!")

            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Registration failed: ${e.message}")
                return@withContext Pair(false, "Registration failed: ${e.message ?: "Please try again."}")
            }
        }
    }

    fun updateContextualProfiles(chatDpUrl: String, postDpUrl: String, channelDpUrl: String, channelAlias: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updatedUser = user.copy(
                chatProfilePictureUrl = chatDpUrl,
                postProfilePictureUrl = postDpUrl,
                channelProfilePictureUrl = channelDpUrl,
                channelAlias = channelAlias
            )
            repository.database.userDao().insertOrUpdateUser(updatedUser)
        }
    }

    fun updateStatusPrivacy(mode: String, list: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updatedUser = user.copy(
                statusPrivacyMode = mode,
                statusPrivacyList = list
            )
            repository.database.userDao().insertOrUpdateUser(updatedUser)
        }
    }

    fun upgradeToPremium() {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updatedUser = user.copy(isPremium = true)
            repository.database.userDao().insertOrUpdateUser(updatedUser)
        }
    }

    fun updateUserProfile(displayName: String, profilePictureUrl: String, bio: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.database.userDao().getCurrentUserOnce() ?: return@launch
            val updatedUser = user.copy(
                displayName = displayName.trim(),
                profilePictureUrl = profilePictureUrl.trim(),
                bio = bio.trim()
            )

            // Save locally in Room
            repository.database.userDao().insertOrUpdateUser(updatedUser)

            // Update all channel avatars for this creator
            repository.database.channelDao().updateChannelAvatarsForCreator(updatedUser.id, updatedUser.profilePictureUrl)

            // Save in Firestore document
            firestoreService.updateUserProfile(
                userId = updatedUser.id,
                displayName = updatedUser.displayName,
                profilePictureUrl = updatedUser.profilePictureUrl,
                bio = updatedUser.bio
            )

            // Update session manager
            sessionManager.updateUserName(updatedUser.displayName)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.database.userDao().getCurrentUserOnce()
            if (user != null) {
                firestoreService.deleteAccount(user.id, user.username)
                repository.database.accountCredentialDao().deleteCredential(user.id)
                repository.database.userDao().updateUserStatus(user.id, "OFFLINE", System.currentTimeMillis())
            }
            repository.database.userDao().clearCurrentUserFlag()
            sessionManager.logout()
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.database.userDao().clearCurrentUserFlag()
            sessionManager.logout()
        }
    }
}
