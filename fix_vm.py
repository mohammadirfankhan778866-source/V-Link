import re

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    vm = f.read()

# 1. Add missing Settings functions
settings_funcs = """
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
            _currentUser.value = updatedUser
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
            _currentUser.value = updatedUser
        }
    }
"""

if "fun updateContextualProfiles" not in vm:
    vm = vm.replace("fun updateUserProfile", settings_funcs + "\n    fun updateUserProfile")


# 2. Update createPost
create_post_regex = r"fun createPost\(.*?\) \{.*?repository\.createPost\(.*?\).*?\n\s*\}"
new_create_post = """fun createPost(content: String, mediaUrl: String = "", mediaType: String = "TEXT", fileExtension: String = "", fileSize: String = "", visibility: String = "PUBLIC") {
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
    }"""
vm = re.sub(create_post_regex, new_create_post, vm, flags=re.DOTALL)


# 3. Update createChannel
create_channel_regex = r"fun createChannel\(name: String, description: String, avatarUrl: String\) \{.*?repository\.createChannel\(.*?\).*?\n\s*\}"
new_create_channel = """fun createChannel(name: String, description: String, avatarUrl: String, visibility: String = "PUBLIC") {
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
    }"""
vm = re.sub(create_channel_regex, new_create_channel, vm, flags=re.DOTALL)


# 4. Also update getOrCreateChannelAndPost to pass visibility
get_or_create_regex = r"fun getOrCreateChannelAndPost\(.*?\) \{"
new_get_or_create = "fun getOrCreateChannelAndPost(channelName: String, content: String, mediaUrl: String, mediaType: String, fileName: String = \"\", fileSize: String = \"\", visibility: String = \"PUBLIC\") {"
vm = re.sub(r"fun getOrCreateChannelAndPost\(.*?\).*?\{", new_get_or_create, vm, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.write(vm)
