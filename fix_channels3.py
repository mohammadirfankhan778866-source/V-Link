import re

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    vm = f.read()

# Fix createChannel
old_create_channel = """    fun createChannel(name: String, description: String, avatarUrl: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val creatorId = user?.id ?: "usr_google_irfan_9075"
            val creatorName = user?.displayName ?: "Mohammad Irfan Khan"
            val finalAvatar = user?.profilePictureUrl?.takeIf { it.isNotBlank() } ?: avatarUrl
            
            repository.createChannel(name, description, creatorId, creatorName, finalAvatar)
        }
    }"""
    
new_create_channel = """    fun createChannel(name: String, description: String, avatarUrl: String, visibility: String = "PUBLIC") {
        viewModelScope.launch {
            val user = currentUser.value
            val creatorId = user?.id ?: "usr_google_irfan_9075"
            val creatorName = user?.channelAlias.takeIf { !it.isNullOrBlank() } ?: user?.displayName ?: "User"
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
    }"""

vm = vm.replace(old_create_channel, new_create_channel)

# Fix sendChannelMessage
old_send_msg = """    fun sendChannelMessage(channelId: String, content: String, mediaUrl: String = "", mediaType: String = "TEXT", fileName: String = "", fileSize: String = "") {
        viewModelScope.launch {
            val user = currentUser.value
            val senderId = user?.id ?: "usr_google_irfan_9075"
            val senderName = user?.displayName ?: "Mohammad Irfan Khan"
            val senderAvatar = user?.profilePictureUrl ?: ""
            
            repository.sendChannelMessage(channelId, senderId, senderName, senderAvatar, content, mediaUrl, mediaType, fileName, fileSize)
        }
    }"""
    
new_send_msg = """    fun sendChannelMessage(channelId: String, content: String, mediaUrl: String = "", mediaType: String = "TEXT", fileName: String = "", fileSize: String = "") {
        viewModelScope.launch {
            val user = currentUser.value
            val senderId = user?.id ?: "usr_google_irfan_9075"
            val senderName = user?.channelAlias.takeIf { !it.isNullOrBlank() } ?: user?.displayName ?: "User"
            val senderAvatar = user?.channelProfilePictureUrl?.takeIf { it.isNotBlank() } ?: user?.profilePictureUrl ?: ""
            
            val msg = ChannelMessageEntity(
                id = java.util.UUID.randomUUID().toString(),
                channelId = channelId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                fileName = fileName,
                fileSize = fileSize
            )
            repository.database.channelDao().insertMessage(msg)
        }
    }"""

vm = vm.replace(old_send_msg, new_send_msg)

# Fix postStatus
old_post_status = """    fun postStatus(mediaUrl: String, caption: String) {
        viewModelScope.launch {
            repository.postStatusStory(mediaUrl, caption)
        }
    }"""
    
new_post_status = """    fun postStatus(mediaUrl: String, caption: String) {
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
    }"""

vm = vm.replace(old_post_status, new_post_status)

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.write(vm)
