import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Add states for Contextual DPs
states = """    var showContextualDpModal by remember { mutableStateOf(false) }
    var showStatusPrivacyModal by remember { mutableStateOf(false) }
    
    // Status Privacy Options
    var statusPrivacyMode by remember { mutableStateOf(currentUser?.statusPrivacyMode ?: "PUBLIC") }
    var statusPrivacyList by remember { mutableStateOf(currentUser?.statusPrivacyList ?: "") }
    
    // DP Options
    var chatDpUrl by remember { mutableStateOf(currentUser?.chatProfilePictureUrl ?: "") }
    var postDpUrl by remember { mutableStateOf(currentUser?.postProfilePictureUrl ?: "") }
    var channelDpUrl by remember { mutableStateOf(currentUser?.channelProfilePictureUrl ?: "") }
    var channelAliasInput by remember { mutableStateOf(currentUser?.channelAlias ?: "") }
"""

content = re.sub(r'    LaunchedEffect\(currentUser\) \{', states + '\n    LaunchedEffect(currentUser) {', content)

# Add elements to the Settings screen layout
settings_menu = """            // Advanced Settings
            Text("ADVANCED SETTINGS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Contextual Profile Pictures") },
                        supportingContent = { Text("Set different DPs for Chats, Posts, and Channels") },
                        leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PulseGreen) },
                        modifier = Modifier.clickable { showContextualDpModal = true }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ListItem(
                        headlineContent = { Text("Status Privacy") },
                        supportingContent = { Text("Control who can see your status updates") },
                        leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = PulseGreen) },
                        modifier = Modifier.clickable { showStatusPrivacyModal = true }
                    )
                }
            }
"""

content = re.sub(r'            // Theme Section', settings_menu + '\n            // Theme Section', content)

# Add Modals at the end of the file
modals = """
        // Contextual DP Modal
        if (showContextualDpModal) {
            AlertDialog(
                onDismissRequest = { showContextualDpModal = false },
                title = { Text("Contextual Profiles", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("You can set different Profile Pictures (DP) based on where you are seen.", fontSize = 13.sp)
                        
                        OutlinedTextField(
                            value = chatDpUrl,
                            onValueChange = { chatDpUrl = it },
                            label = { Text("Chat DP (URL)") },
                            placeholder = { Text("Leave blank to use main DP") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = postDpUrl,
                            onValueChange = { postDpUrl = it },
                            label = { Text("Post DP (URL)") },
                            placeholder = { Text("Leave blank to use main DP") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = channelDpUrl,
                            onValueChange = { channelDpUrl = it },
                            label = { Text("Channel DP (URL)") },
                            placeholder = { Text("Leave blank to use main DP") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = channelAliasInput,
                            onValueChange = { channelAliasInput = it },
                            label = { Text("Channel Alias (Username)") },
                            placeholder = { Text("e.g. MySecretAlias") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateContextualProfiles(chatDpUrl, postDpUrl, channelDpUrl, channelAliasInput)
                            showContextualDpModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showContextualDpModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Status Privacy Modal
        if (showStatusPrivacyModal) {
            AlertDialog(
                onDismissRequest = { showStatusPrivacyModal = false },
                title = { Text("Status Privacy", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Who can see my status updates?", fontSize = 13.sp)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = statusPrivacyMode == "PUBLIC", onClick = { statusPrivacyMode = "PUBLIC" })
                            Text("My Contacts (Public)")
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = statusPrivacyMode == "EXCEPT", onClick = { statusPrivacyMode = "EXCEPT" })
                            Text("My Contacts Except...")
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = statusPrivacyMode == "ONLY_SHARE_WITH", onClick = { statusPrivacyMode = "ONLY_SHARE_WITH" })
                            Text("Only Share With...")
                        }
                        
                        if (statusPrivacyMode != "PUBLIC") {
                            OutlinedTextField(
                                value = statusPrivacyList,
                                onValueChange = { statusPrivacyList = it },
                                label = { Text(if (statusPrivacyMode == "EXCEPT") "Hide from (Usernames)" else "Share with (Usernames)") },
                                placeholder = { Text("user1, user2") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Enter usernames separated by commas.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateStatusPrivacy(statusPrivacyMode, statusPrivacyList)
                            showStatusPrivacyModal = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Text("Save Privacy")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStatusPrivacyModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
"""

content = re.sub(r'        // Edit Profile Dialog', modals + '\n        // Edit Profile Dialog', content)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
