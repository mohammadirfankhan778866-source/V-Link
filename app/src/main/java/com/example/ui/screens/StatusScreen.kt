package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.StatusStoryEntity
import com.example.ui.components.PulseAvatar
import com.example.ui.theme.PulseGreen
import com.example.ui.viewmodels.MainViewModel
import com.example.util.MediaUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(viewModel: MainViewModel) {
    val statuses by viewModel.statuses.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeStatusViewer by viewModel.activeStatusViewer.collectAsState()
    
    var showPostStatusModal by remember { mutableStateOf(false) }
    var captionInput by remember { mutableStateOf("") }
    var statusMediaUrl by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Active Status Viewer Dialog
    activeStatusViewer?.let { activeStatus ->
        StatusStoryViewerModal(
            status = activeStatus,
            onDismiss = { viewModel.closeStatusViewer() }
        )
    }

    val statusPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = MediaUtils.copyUriToInternalStorage(context, uri, "status_media")
            if (localPath != null) {
                statusMediaUrl = "file://\$localPath"
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
                            if (myStatus != null) viewModel.openStatusViewer(myStatus)
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
    }

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
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        OutlinedButton(
                            onClick = { 
                                statusPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Photo or Video")
                        }
                    }

                    OutlinedTextField(
                        value = captionInput,
                        onValueChange = { captionInput = it },
                        label = { Text("Caption (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (statusMediaUrl.isNotEmpty()) {
                            val status = StatusStoryEntity(
                                id = UUID.randomUUID().toString(),
                                userId = currentUser?.id ?: "usr_my_id",
                                userName = currentUser?.displayName ?: "Me",
                                userAvatar = currentUser?.profilePictureUrl ?: "",
                                mediaUrl = statusMediaUrl,
                                caption = captionInput,
                                timestamp = System.currentTimeMillis(),
                                isViewed = true,
                                isMine = true
                            )
                            viewModel.postStatus(statusMediaUrl, captionInput)
                            showPostStatusModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseGreen),
                    enabled = statusMediaUrl.isNotEmpty()
                ) {
                    Text("Post Status")
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
            hasStory = !status.isViewed
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(status.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(formattedTime, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusStoryViewerModal(
    status: StatusStoryEntity,
    onDismiss: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(status.id) {
        val totalMs = 5000L
        val interval = 50L
        val steps = (totalMs / interval).toInt()
        for (i in 1..steps) {
            kotlinx.coroutines.delay(interval)
            progress = i.toFloat() / steps
        }
        onDismiss()
    }

    val formattedTime = remember(status.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(status.timestamp))
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (status.mediaUrl.isNotEmpty()) {
                AsyncImage(
                    model = status.mediaUrl,
                    contentDescription = "Status Story",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF075E54)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status.caption,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulseAvatar(imageUrl = status.userAvatar, name = status.userName, size = 42.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(status.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(formattedTime, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            if (status.mediaUrl.isNotEmpty() && status.caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(24.dp)
                ) {
                    Text(
                        text = status.caption,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
