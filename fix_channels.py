import re

with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "r") as f:
    content = f.read()

# 1. Fix createChannel
old_create = """val newChannel = ChannelEntity(
                                id = UUID.randomUUID().toString(),
                                name = channelNameInput.trim(),
                                description = channelDescInput.trim(),
                                avatarUrl = channelAvatarInput.ifBlank { "https://picsum.photos/seed/${channelNameInput.trim()}/300/300" },
                                creatorId = currentUserId,
                                creatorName = currentUser?.displayName ?: "User",
                                followerCount = 1,
                                isFollowedByMe = true
                            )
                            viewModel.createChannel(newChannel)"""
                            
new_create = """viewModel.createChannel(
                                name = channelNameInput.trim(),
                                description = channelDescInput.trim(),
                                avatarUrl = channelAvatarInput.ifBlank { "https://picsum.photos/seed/${channelNameInput.trim()}/300/300" }
                            )"""
                            
content = content.replace(old_create, new_create)

# 2. Fix getChannelMessages
content = content.replace('viewModel.getChannelMessages', 'viewModel.getMessagesForChannel')

# 3. Fix postChannelMessage
content = content.replace('viewModel.postChannelMessage', 'viewModel.sendChannelMessage')

with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "w") as f:
    f.write(content)
