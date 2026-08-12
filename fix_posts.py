import re

with open("app/src/main/java/com/example/ui/screens/PostsScreen.kt", "r") as f:
    content = f.read()

# Add state
content = content.replace('var postChannelNameInput by remember { mutableStateOf("") }', 
    'var postChannelNameInput by remember { mutableStateOf("") }\n    var postVisibility by remember { mutableStateOf("PUBLIC") }')

# Add to modal reset
content = content.replace('attachedFileSize = ""\n                },',
    'attachedFileSize = ""\n                    postVisibility = "PUBLIC"\n                },')
content = content.replace('postChannelNameInput = ""\n                        }',
    'postChannelNameInput = ""\n                            postVisibility = "PUBLIC"\n                        }')

# Add dropdown/toggle for visibility in the modal
toggle_ui = """
                        // Visibility Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Visibility: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = postVisibility == "PUBLIC",
                                onClick = { postVisibility = "PUBLIC" },
                                label = { Text("Public") },
                                leadingIcon = { if (postVisibility == "PUBLIC") Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = postVisibility == "FRIENDS_ONLY",
                                onClick = { postVisibility = "FRIENDS_ONLY" },
                                label = { Text("Friends Only") },
                                leadingIcon = { if (postVisibility == "FRIENDS_ONLY") Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
"""

content = content.replace('// Attach options', toggle_ui + '\n                        // Attach options')

# Add parameter to viewmodel
# Wait, let me just add visibility to the createPost function in MainViewModel and call site

with open("app/src/main/java/com/example/ui/screens/PostsScreen.kt", "w") as f:
    f.write(content)
