import re

with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "r") as f:
    content = f.read()

pattern = r'val newChannel = ChannelEntity\(.*?isFollowedByMe = true\n\s*\)\n\s*viewModel\.createChannel\(newChannel\)'
replacement = '''viewModel.createChannel(
                                name = channelNameInput.trim(),
                                description = channelDescInput.trim(),
                                avatarUrl = channelAvatarInput.ifBlank { "https://picsum.photos/seed/${channelNameInput.trim()}/300/300" }
                            )'''

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "w") as f:
    f.write(content)
