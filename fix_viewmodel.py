import re

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    content = f.read()

funcs = """
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

content = content.replace("fun updateUserProfile(displayName: String, avatarUrl: String, bio: String) {", funcs + "\n    fun updateUserProfile(displayName: String, avatarUrl: String, bio: String) {")

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.write(content)
