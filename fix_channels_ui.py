import re

with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "r") as f:
    content = f.read()

imports = """import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Group
"""
if "import androidx.compose.material.icons.filled.Public" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.*", "import androidx.compose.material.icons.filled.*\n" + imports)

# Filter logic
content = content.replace(
    'val discoverChannels = filteredChannels.filter { !it.isFollowedByMe && it.creatorId != currentUserId }',
    'val discoverChannels = filteredChannels.filter { !it.isFollowedByMe && it.creatorId != currentUserId && it.visibility == "PUBLIC" }'
)

# Modal state
content = content.replace('var channelAvatarInput by remember { mutableStateOf("") }',
    'var channelAvatarInput by remember { mutableStateOf("") }\n    var channelVisibility by remember { mutableStateOf("PUBLIC") }')

# Modal reset
content = content.replace(
    'channelAvatarInput = ""\n                        }',
    'channelAvatarInput = ""\n                            channelVisibility = "PUBLIC"\n                        }'
)

# Modal toggle
toggle_ui = """
                    // Visibility Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Visibility: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = channelVisibility == "PUBLIC",
                            onClick = { channelVisibility = "PUBLIC" },
                            label = { Text("Public") },
                            leadingIcon = { if (channelVisibility == "PUBLIC") Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = channelVisibility == "FRIENDS_ONLY",
                            onClick = { channelVisibility = "FRIENDS_ONLY" },
                            label = { Text("Private / Friends Only") },
                            leadingIcon = { if (channelVisibility == "FRIENDS_ONLY") Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
"""

content = content.replace(
    'OutlinedTextField(\n                        value = channelDescInput',
    toggle_ui + 'OutlinedTextField(\n                        value = channelDescInput'
)

# Call to createChannel
content = content.replace(
    'avatarUrl = channelAvatarInput.ifBlank { "https://picsum.photos/seed/${channelNameInput.trim()}/300/300" }\n                            )',
    'avatarUrl = channelAvatarInput.ifBlank { "https://picsum.photos/seed/${channelNameInput.trim()}/300/300" },\n                                visibility = channelVisibility\n                            )'
)

with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "w") as f:
    f.write(content)
