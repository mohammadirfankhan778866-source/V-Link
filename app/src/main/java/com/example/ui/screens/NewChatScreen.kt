package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
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
import com.example.ui.components.PulseAvatar
import com.example.ui.theme.PulseGreen
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatModal(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()
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
                    text = if (isCreatingGroup) "New Group Chat" else "Select Contact",
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
                    shape = CircleShape
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Participants:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(contacts, key = { it.id }) { contact ->
                        val isSelected = selectedParticipantIds.contains(contact.id)
                        ListItem(
                            headlineContent = { Text(contact.displayName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(contact.bio, fontSize = 12.sp) },
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
                var newContactName by remember { mutableStateOf("") }
                var newContactUsername by remember { mutableStateOf("") }
                var newContactBio by remember { mutableStateOf("") }

                if (showAddContactDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddContactDialog = false },
                        title = { Text("Save & Start Chat") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Write the handle (@username) of the person you want to message or call, and save a Nick Name for remembrance.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = newContactUsername,
                                    onValueChange = { newContactUsername = it },
                                    label = { Text("1. Handle (@username)") },
                                    placeholder = { Text("e.g. @irfan_vlink") },
                                    leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("add_contact_username_input")
                                )

                                OutlinedTextField(
                                    value = newContactName,
                                    onValueChange = { newContactName = it },
                                    label = { Text("2. Nick Name") },
                                    placeholder = { Text("e.g. Mohammad Irfan Khan") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("add_contact_nickname_input")
                                )

                                OutlinedTextField(
                                    value = newContactBio,
                                    onValueChange = { newContactBio = it },
                                    label = { Text("Status / Bio (Optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val handle = newContactUsername.ifBlank { newContactName.lowercase().replace(" ", "_") }
                                    val nickname = newContactName.ifBlank { newContactUsername.removePrefix("@") }
                                    if (handle.isNotBlank()) {
                                        viewModel.addContact(nickname, handle, newContactBio)
                                        showAddContactDialog = false
                                        onDismiss()
                                    }
                                },
                                enabled = newContactUsername.isNotBlank() || newContactName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                            ) {
                                Text("Save & Chat", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddContactDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text("New Contact", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Add a contact by name or handle") },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PulseGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                        }
                    },
                    modifier = Modifier
                        .clickable { showAddContactDialog = true }
                        .testTag("add_new_contact_item")
                )

                ListItem(
                    headlineContent = { Text("New Group Chat", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Create a group with contacts") },
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

                Text("CONTACTS ON PULSE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No contacts yet. Tap 'New Contact' above to add someone!",
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
