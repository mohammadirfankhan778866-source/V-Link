package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.UserEntity
import com.example.ui.components.PulseAvatar
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatModal(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isCreatingGroup by remember { mutableStateOf(false) }
    var groupTitle by remember { mutableStateOf("") }
    val selectedParticipantIds = remember { mutableStateListOf<String>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isCreatingGroup) "New Group Chat" else "Start a Conversation",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isCreatingGroup) {
                OutlinedTextField(
                    value = groupTitle,
                    onValueChange = { groupTitle = it },
                    label = { Text("Group Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Registered Participants:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(contacts, key = { it.id }) { contact ->
                        val isSelected = selectedParticipantIds.contains(contact.id)
                        ListItem(
                            headlineContent = { Text(contact.displayName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(contact.username, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary) },
                            leadingContent = { PulseAvatar(imageUrl = contact.profilePictureUrl, name = contact.displayName) },
                            trailingContent = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedParticipantIds.add(contact.id)
                                        else selectedParticipantIds.remove(contact.id)
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (isSelected) selectedParticipantIds.remove(contact.id)
                                else selectedParticipantIds.add(contact.id)
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (groupTitle.isNotBlank()) {
                            viewModel.createGroupChat(groupTitle, selectedParticipantIds.toList())
                            onDismiss()
                        }
                    },
                    enabled = groupTitle.isNotBlank() && selectedParticipantIds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_group_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                ) {
                    Text("Create Group (${selectedParticipantIds.size})", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                var showAddContactDialog by remember { mutableStateOf(false) }
                var searchUsernameInput by remember { mutableStateOf("") }
                var isLookingUp by remember { mutableStateOf(false) }
                var foundUser by remember { mutableStateOf<UserEntity?>(null) }
                var lookupError by remember { mutableStateOf<String?>(null) }
                var lookupJob by remember { mutableStateOf<Job?>(null) }

                if (showAddContactDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showAddContactDialog = false
                            foundUser = null
                            lookupError = null
                            searchUsernameInput = ""
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = PulseGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Find Registered User")
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "Enter the unique @username of the person you want to contact. V-Link will automatically look up their profile and nickname.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = searchUsernameInput,
                                    onValueChange = { input ->
                                        searchUsernameInput = input
                                        foundUser = null
                                        lookupError = null
                                        lookupJob?.cancel()

                                        val clean = input.trim().removePrefix("@")
                                        if (clean.length >= 2) {
                                            isLookingUp = true
                                            lookupJob = coroutineScope.launch {
                                                delay(400)
                                                val user = viewModel.findRegisteredUserByUsername(clean)
                                                isLookingUp = false
                                                if (user != null) {
                                                    val myUser = viewModel.currentUser.value
                                                    if (user.id == myUser?.id || user.username.equals(myUser?.username, ignoreCase = true)) {
                                                        lookupError = "You cannot start a conversation with your own username."
                                                        foundUser = null
                                                    } else {
                                                        foundUser = user
                                                        lookupError = null
                                                    }
                                                } else {
                                                    lookupError = "No user found with @$clean. Only real registered V-Link accounts can be contacted."
                                                    foundUser = null
                                                }
                                            }
                                        } else {
                                            isLookingUp = false
                                        }
                                    },
                                    label = { Text("Username Handle") },
                                    placeholder = { Text("e.g. @username") },
                                    leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = PulseGreen, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp)) },
                                    trailingIcon = {
                                        if (isLookingUp) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PulseGreen)
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("add_contact_username_input")
                                )

                                // Auto-fetched profile card when found
                                if (foundUser != null) {
                                    val user = foundUser!!
                                    Surface(
                                        color = PulseGreen.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PulseGreen.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PulseAvatar(imageUrl = user.profilePictureUrl, name = user.displayName, size = 44.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = user.displayName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Default.Verified, contentDescription = "Verified", tint = PulseGreen, modifier = Modifier.size(14.dp))
                                                }
                                                Text(
                                                    text = if (user.username.startsWith("@")) user.username else "@${user.username}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (user.bio.isNotBlank()) {
                                                    Text(
                                                        text = user.bio,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Error message
                                if (lookupError != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = lookupError ?: "",
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val user = foundUser
                                    if (user != null) {
                                        viewModel.startChatWithContact(user)
                                        showAddContactDialog = false
                                        onDismiss()
                                    }
                                },
                                enabled = foundUser != null,
                                colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Chat", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showAddContactDialog = false
                                foundUser = null
                                lookupError = null
                                searchUsernameInput = ""
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text("New Contact by @username", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Find & add registered users by their handle") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PulseGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.White)
                        }
                    },
                    modifier = Modifier
                        .clickable { showAddContactDialog = true }
                        .testTag("add_new_contact_item")
                )

                ListItem(
                    headlineContent = { Text("New Group Chat", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Create a group with existing contacts") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PulseGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color.White)
                        }
                    },
                    modifier = Modifier.clickable { isCreatingGroup = true }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("YOUR CONTACTS ON V-LINK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No contacts yet. Tap 'New Contact by @username' above to find registered users!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(contacts, key = { it.id }) { contact ->
                            val handleText = if (contact.username.startsWith("@")) contact.username else "@${contact.username}"
                            ListItem(
                                headlineContent = { Text(contact.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                                supportingContent = {
                                    Column {
                                        Text(handleText, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                        if (contact.bio.isNotBlank()) {
                                            Text(contact.bio, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                leadingContent = { PulseAvatar(imageUrl = contact.profilePictureUrl, name = contact.displayName, isOnline = true) },
                                modifier = Modifier.clickable {
                                    viewModel.startChatWithContact(contact)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
