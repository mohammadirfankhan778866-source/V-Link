import re

# Fix ChannelsScreen
with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "r") as f:
    c_content = f.read()

c_content = c_content.replace('import androidx.compose.material.icons.filled.Campaign', 'import androidx.compose.material.icons.filled.Campaign\nimport androidx.compose.material.icons.filled.CameraAlt')
c_content = c_content.replace('creatorId = currentUserId,', f'creatorId = currentUserId,\n                                creatorName = currentUser?.displayName ?: "User",')
c_content = c_content.replace('Text(msg.text, fontSize = 15.sp)', 'Text(msg.content, fontSize = 15.sp)')

with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "w") as f:
    f.write(c_content)

# Fix StatusScreen
with open("app/src/main/java/com/example/ui/screens/StatusScreen.kt", "r") as f:
    s_content = f.read()

s_content = s_content.replace('uploaderId =', 'userId =')
s_content = s_content.replace('uploaderName =', 'userName =')
s_content = s_content.replace('uploaderAvatarUrl =', 'userAvatar =')
s_content = s_content.replace('status.uploaderName', 'status.userName')
s_content = s_content.replace('status.uploaderAvatarUrl', 'status.userAvatar')

# Fix viewModel.postStatus
s_content = s_content.replace('viewModel.postStatus(status)', 'viewModel.postStatus(statusMediaUrl, captionInput)')

with open("app/src/main/java/com/example/ui/screens/StatusScreen.kt", "w") as f:
    f.write(s_content)
