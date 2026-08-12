import re

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    vm = f.read()

# Update inside getOrCreateChannelAndPost
old_create = """                val newChannel = ChannelEntity(
                    id = newChannelId,
                    name = channelName.trim(),
                    description = "Official channel for $channelName uploads.",
                    creatorId = creatorId,
                    creatorName = creatorName,
                    avatarUrl = finalAvatar,
                    followerCount = 1,
                    isFollowedByMe = true
                )"""

new_create = """                val newChannel = ChannelEntity(
                    id = newChannelId,
                    name = channelName.trim(),
                    description = "Official channel for $channelName uploads.",
                    creatorId = creatorId,
                    creatorName = user?.channelAlias?.takeIf { it.isNotBlank() } ?: creatorName,
                    avatarUrl = user?.channelProfilePictureUrl?.takeIf { it.isNotBlank() } ?: finalAvatar,
                    followerCount = 1,
                    isFollowedByMe = true,
                    visibility = visibility
                )"""

vm = vm.replace(old_create, new_create)

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.write(vm)
