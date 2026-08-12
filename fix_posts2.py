import re

with open("app/src/main/java/com/example/ui/screens/PostsScreen.kt", "r") as f:
    content = f.read()

# Add imports
imports = """import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.FilterChip"""

if "import androidx.compose.material.icons.filled.Public" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.*", "import androidx.compose.material.icons.filled.*\n" + imports)

# Update createPost call
content = content.replace(
    'fileSize = attachedFileSize',
    'fileSize = attachedFileSize,\n                                    visibility = postVisibility'
)

with open("app/src/main/java/com/example/ui/screens/PostsScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    vm = f.read()

# Update createPost signature and usage
old_create_post = """    fun createPost(content: String, mediaUrl: String, mediaType: String, fileExtension: String, fileSize: String) {
        viewModelScope.launch {
            val user = currentUser.value
            val userId = user?.id ?: "usr_google_irfan_9075"
            val userName = user?.displayName ?: "Mohammad Irfan Khan"
            val userAvatar = user?.profilePictureUrl ?: "https://picsum.photos/seed/irfan/300/300"
            
            repository.createPost(userId, userName, userAvatar, content, mediaUrl, mediaType, fileExtension, fileSize)
        }
    }"""

new_create_post = """    fun createPost(content: String, mediaUrl: String, mediaType: String, fileExtension: String, fileSize: String, visibility: String = "PUBLIC") {
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

vm = vm.replace(old_create_post, new_create_post)

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.write(vm)
