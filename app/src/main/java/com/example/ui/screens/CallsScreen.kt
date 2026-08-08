package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CallLogEntity
import com.example.data.models.CallType
import com.example.ui.components.PulseAvatar
import com.example.ui.theme.CallEndRed
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
import com.example.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(viewModel: MainViewModel) {
    val calls by viewModel.calls.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    var showNewCallDialog by remember { mutableStateOf(false) }
    var selectedContactName by remember { mutableStateOf("") }
    var selectedContactAvatar by remember { mutableStateOf("") }

    if (showNewCallDialog) {
        AlertDialog(
            onDismissRequest = { showNewCallDialog = false },
            title = { Text("Start New Call") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (contacts.isNotEmpty()) {
                        Text("Select Contact:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(contacts, key = { it.id }) { contact ->
                                ListItem(
                                    headlineContent = { Text(contact.displayName, fontWeight = FontWeight.Bold) },
                                    leadingContent = { PulseAvatar(imageUrl = contact.profilePictureUrl, name = contact.displayName) },
                                    modifier = Modifier.clickable {
                                        selectedContactName = contact.displayName
                                        selectedContactAvatar = contact.profilePictureUrl
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = selectedContactName,
                        onValueChange = {
                            selectedContactName = it
                            selectedContactAvatar = "https://picsum.photos/seed/$it/300/300"
                        },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (selectedContactName.isNotBlank()) {
                                viewModel.startCall(selectedContactName, selectedContactAvatar, false)
                                showNewCallDialog = false
                            }
                        },
                        enabled = selectedContactName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice")
                    }

                    Button(
                        onClick = {
                            if (selectedContactName.isNotBlank()) {
                                viewModel.startCall(selectedContactName, selectedContactAvatar, true)
                                showNewCallDialog = false
                            }
                        },
                        enabled = selectedContactName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Video")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCallDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calls", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewCallDialog = true },
                containerColor = PulseGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("start_call_fab")
            ) {
                Icon(Icons.Default.Call, contentDescription = "New Call")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("RECENT CALLS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            }

            if (calls.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No recent call logs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(calls, key = { it.id }) { call ->
                    CallLogItemRow(
                        call = call,
                        onCallClick = { viewModel.startCall(call.contactName, call.contactAvatar, call.callType == CallType.VIDEO.name) }
                    )
                }
            }
        }
    }
}

@Composable
fun CallLogItemRow(call: CallLogEntity, onCallClick: () -> Unit) {
    val formattedTime = remember(call.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(call.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulseAvatar(imageUrl = call.contactAvatar, name = call.contactName, size = 52.dp)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Nick Name Bolded
            Text(call.contactName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (call.isMissed) CallEndRed else MaterialTheme.colorScheme.onSurface)

            // Username handle at the bottom of Nick Name (Instagram-style)
            val handleText = if (call.contactUsername.isNotEmpty()) {
                if (call.contactUsername.startsWith("@")) call.contactUsername else "@${call.contactUsername}"
            } else {
                "@${call.contactName.lowercase().replace(" ", "_")}"
            }

            Text(
                text = handleText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = VLinkCyan
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (call.isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                    contentDescription = null,
                    tint = if (call.isMissed) CallEndRed else PulseGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(formattedTime, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        IconButton(onClick = onCallClick) {
            Icon(
                imageVector = if (call.callType == CallType.VIDEO.name) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Call",
                tint = PulseGreen
            )
        }
    }
}
