package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.models.StatusStoryEntity
import com.example.ui.components.PulseAvatar
import com.example.ui.theme.PulseGreen
import com.example.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.util.MediaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(viewModel: MainViewModel) {
    val statuses by viewModel.statuses.collectAsState()
    val activeStatusViewer by viewModel.activeStatusViewer.collectAsState()
    var showPostStatusModal by remember { mutableStateOf(false) }
    var captionInput by remember { mutableStateOf("") }
    var statusMediaUrl by remember { mutableStateOf("") }
    val context = LocalContext.current

    val statusPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = MediaUtils.copyUriToInternalStorage(context, uri, "status_media")
            if (localPath != null) {
                statusMediaUrl = "file://$localPath"
            }
        }
    }

    val unviewedStatuses = remember(statuses) { statuses.filter { !it.isViewed && !it.isMine } }
    val viewedStatuses = remember(statuses) { statuses.filter { it.isViewed && !it.isMine } }
    val myStatus = remember(statuses) { statuses.firstOrNull { it.isMine } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Updates", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPostStatusModal = true },
                containerColor = PulseGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("post_status_fab")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Add Status")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("STATUS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // My Status Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStatus != null) viewModel.openStatusViewer(myStatus!!)
                            else showPostStatusModal = true
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        PulseAvatar(
                            imageUrl = myStatus?.mediaUrl ?: "https://picsum.photos/seed/irfan/300/300",
                            name = "My Status",
                            size = 56.dp,
                            hasStory = myStatus != null
                        )
                        if (myStatus == null) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(PulseGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text("My status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (myStatus != null) "Tap to view status update" else "Tap to add status update",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Unviewed Statuses
            if (unviewedStatuses.isNotEmpty()) {
                item {
                    Text("RECENT UPDATES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PulseGreen, modifier = Modifier.padding(top = 8.dp))
                }
                items(unviewedStatuses, key = { it.id }) { status ->
                    StatusRowItem(status = status, onClick = { viewModel.openStatusViewer(status) })
                }
            }

            // Viewed Statuses
            if (viewedStatuses.isNotEmpty()) {
                item {
                    Text("VIEWED UPDATES", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
                items(viewedStatuses, key = { it.id }) { status ->
                    StatusRowItem(status = status, onClick = { viewModel.openStatusViewer(status) })
                }
            }
        }

        // Fullscreen Story Viewer Modal
        if (activeStatusViewer != null) {
            val status = activeStatusViewer!!
            var commentInput by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { viewModel.closeStatusViewer() }) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = status.mediaUrl,
                            contentDescription = status.caption,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Top bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopStart),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulseAvatar(imageUrl = status.userAvatar, name = status.userName, size = 40.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(status.userName, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Today", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { viewModel.closeStatusViewer() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        // Bottom caption
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(16.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            Column {
                                if (status.caption.isNotBlank()) {
                                    Text(status.caption, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                if (!status.isMine) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = commentInput,
                                            onValueChange = { commentInput = it },
                                            placeholder = { Text("Reply...", color = Color.White.copy(alpha = 0.6f)) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.White,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                cursorColor = PulseGreen
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { 
                                            if (commentInput.isNotBlank()) {
                                                val chatId = "chat_" + status.userId.replace("usr_", "")
                                                // Create a chat if it doesn't exist
                                                viewModel.startChatWithContact(
                                                    com.example.data.models.UserEntity(
                                                        id = status.userId,
                                                        displayName = status.userName,
                                                        username = "",
                                                        email = "",
                                                        profilePictureUrl = status.userAvatar,
                                                        bio = ""
                                                    )
                                                )
                                                viewModel.sendMessage(
                                                    chatId = chatId,
                                                    content = "Replying to your status: ${if(status.caption.isNotBlank()) status.caption else "Media"}\n\n$commentInput"
                                                )
                                                commentInput = ""
                                                viewModel.closeStatusViewer()
                                                viewModel.openChatDetail(chatId)
                                            }
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Reply", tint = PulseGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Post Status Modal
        if (showPostStatusModal) {
            AlertDialog(
                onDismissRequest = { showPostStatusModal = false },
                title = { Text("Add Status Story") },
                text = {
                    Column {
                        if (statusMediaUrl.isNotEmpty()) {
                            AsyncImage(
                                model = statusMediaUrl,
                                contentDescription = "Status Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        Button(
                            onClick = { 
                                statusPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = PulseGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Photo/Video", color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = captionInput,
                            onValueChange = { captionInput = it },
                            label = { Text("Status Caption") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalMediaUrl = if (statusMediaUrl.isNotEmpty()) statusMediaUrl else "https://picsum.photos/seed/mystatus_${System.currentTimeMillis()}/800/1200"
                            if (captionInput.isNotBlank() || statusMediaUrl.isNotEmpty()) {
                                viewModel.postStatus(finalMediaUrl, captionInput)
                                showPostStatusModal = false
                                captionInput = ""
                                statusMediaUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Text("Post Story", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPostStatusModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun StatusRowItem(status: StatusStoryEntity, onClick: () -> Unit) {
    val formattedTime = remember(status.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(status.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulseAvatar(
            imageUrl = status.userAvatar,
            name = status.userName,
            size = 56.dp,
            hasStory = true,
            storySeen = status.isViewed
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(status.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(formattedTime, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
