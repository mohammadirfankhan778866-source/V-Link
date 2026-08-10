package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.models.PostEntity
import com.example.ui.components.PulseAvatar
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
import com.example.ui.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.MediaUtils
import androidx.compose.ui.text.style.TextAlign
import android.net.Uri
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.collectAsState()
    var showCreatePostModal by remember { mutableStateOf(false) }
    var postTextInput by remember { mutableStateOf("") }
    var attachedFileUrl by remember { mutableStateOf("") }
    var attachedMediaType by remember { mutableStateOf("TEXT") }
    var attachedFileName by remember { mutableStateOf("") }
    var attachedFileSize by remember { mutableStateOf("") }
    var postChannelNameInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = MediaUtils.copyUriToInternalStorage(context, uri, "post_attachments")
            if (localPath != null) {
                attachedFileUrl = "file://$localPath"
                attachedMediaType = "IMAGE"
                attachedFileName = uri.lastPathSegment ?: "image.jpg"
                attachedFileSize = "1.4 MB"
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = MediaUtils.copyUriToInternalStorage(context, uri, "post_videos")
            if (localPath != null) {
                attachedFileUrl = "file://$localPath"
                attachedMediaType = "VIDEO"
                attachedFileName = uri.lastPathSegment ?: "video.mp4"
                attachedFileSize = "12.5 MB"
            }
        }
    }

    val anyFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val resolver = context.contentResolver
            var displayName = "file.zip"
            var sizeStr = "2.8 MB"
            try {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) displayName = cursor.getString(nameIdx)
                        if (sizeIdx != -1) {
                            val bytes = cursor.getLong(sizeIdx)
                            sizeStr = java.lang.String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val localPath = MediaUtils.copyUriToInternalStorage(context, uri, "post_files")
            if (localPath != null) {
                attachedFileUrl = "file://$localPath"
                attachedMediaType = "DOCUMENT"
                attachedFileName = displayName
                attachedFileSize = sizeStr
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Feed", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreatePostModal = true },
                containerColor = PulseGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_post_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreatePostModal = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val userState = viewModel.currentUser.collectAsState()
                    PulseAvatar(
                        imageUrl = userState.value?.profilePictureUrl ?: "",
                        name = userState.value?.displayName ?: "User",
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(19.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            "Share text, photos, video, files, or games...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Feed,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            "No posts yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Be the first one to post to the global feed!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onLikeToggle = { viewModel.toggleLikePost(post) }
                        )
                    }
                }
            }
        }

        // Create Post Dialog
        if (showCreatePostModal) {
            AlertDialog(
                onDismissRequest = {
                    showCreatePostModal = false
                    postTextInput = ""
                    attachedFileUrl = ""
                    attachedMediaType = "TEXT"
                    attachedFileName = ""
                    attachedFileSize = ""
                },
                title = { Text("Create Public Post", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = postTextInput,
                            onValueChange = { postTextInput = it },
                            placeholder = { Text("What's on your mind? Share files, videos, or games...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            maxLines = 4
                        )

                        if (attachedFileUrl.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            ) {
                                if (attachedMediaType == "IMAGE") {
                                    AsyncImage(
                                        model = attachedFileUrl,
                                        contentDescription = "Attachment preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (attachedMediaType == "VIDEO") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Video Attached", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(attachedFileName, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                } else if (attachedMediaType == "GAME") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Gamepad, contentDescription = null, tint = PulseGreen, modifier = Modifier.size(40.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Game Attached: $attachedFileName", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Playable directly inside post card!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(40.dp), tint = PulseGreen)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(attachedFileName, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 13.sp)
                                        Text(attachedFileSize, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        attachedFileUrl = ""
                                        attachedMediaType = "TEXT"
                                        attachedFileName = ""
                                        attachedFileSize = ""
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove file", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            // Mandatory Channel Name input
                            Text(
                                "Channel Name Required *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                            Text(
                                "To upload photos, videos, or files on your account, please specify a Channel name to sync with your account.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = postChannelNameInput,
                                onValueChange = { postChannelNameInput = it },
                                label = { Text("Channel Name") },
                                placeholder = { Text("e.g. My Tech Hub") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Attach options
                        Text("ATTACH TO POST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PulseGreen, modifier = Modifier.padding(top = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    filePickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = PulseGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add Photo", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Button(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add Video", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    anyFilePickerLauncher.launch("*/*")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = VLinkCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add File", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            var showGameSelector by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    showGameSelector = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Gamepad, contentDescription = null, tint = Color.Magenta, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add Game", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (showGameSelector) {
                                AlertDialog(
                                    onDismissRequest = { showGameSelector = false },
                                    title = { Text("Choose Game", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text("Select an interactive game challenge. Users can play it inside their feeds!", fontSize = 13.sp)
                                            
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        attachedFileUrl = "tic_tac_toe"
                                                        attachedMediaType = "GAME"
                                                        attachedFileName = "Tic-Tac-Toe"
                                                        attachedFileSize = "Compose Game"
                                                        showGameSelector = false
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Gamepad, contentDescription = null, tint = PulseGreen)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text("Tic-Tac-Toe Duel ⚔️", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text("Classic strategy board game vs AI.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        attachedFileUrl = "tap_challenge"
                                                        attachedMediaType = "GAME"
                                                        attachedFileName = "Pulse Tap Speedrun"
                                                        attachedFileSize = "Compose Game"
                                                        showGameSelector = false
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Red)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text("Pulse Tap Challenge ⚡", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text("Test your reaction speed in 10s!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showGameSelector = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }

                        // Presets
                        Text("PRESETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PulseGreen, modifier = Modifier.padding(top = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("Demo Video", "https://www.w3schools.com/html/mov_bbb.mp4", "VIDEO"),
                                Triple("Project Spec.pdf", "file://documents/pulse_spec_document.pdf", "DOCUMENT"),
                                Triple("Workspace", "https://picsum.photos/seed/workspace_post/800/600", "IMAGE")
                            ).forEach { (label, url, type) ->
                                FilterChip(
                                    selected = attachedFileUrl == url,
                                    onClick = {
                                        attachedFileUrl = url
                                        attachedMediaType = type
                                        attachedFileName = if (type == "VIDEO") "mov_bbb.mp4" else if (type == "DOCUMENT") "pulse_spec_document.pdf" else "workspace.jpg"
                                        attachedFileSize = if (type == "VIDEO") "3.4 MB" else if (type == "DOCUMENT") "1.2 MB" else "2.1 MB"
                                    },
                                    label = { Text(label, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    val isEnabled = if (attachedFileUrl.isNotEmpty()) postChannelNameInput.isNotBlank() else (postTextInput.isNotBlank() || attachedFileUrl.isNotEmpty())
                    Button(
                        enabled = isEnabled,
                        onClick = {
                            if (postTextInput.isNotBlank() || attachedFileUrl.isNotEmpty()) {
                                viewModel.createPost(
                                    content = postTextInput,
                                    mediaUrl = attachedFileUrl,
                                    mediaType = attachedMediaType,
                                    fileExtension = if (attachedMediaType == "DOCUMENT") "pdf" else if (attachedMediaType == "VIDEO") "mp4" else if (attachedMediaType == "GAME") "game" else "jpg",
                                    fileSize = attachedFileSize
                                )
                                
                                // Automatically sync upload to user's specified channel
                                if (attachedFileUrl.isNotEmpty() && postChannelNameInput.isNotBlank()) {
                                    viewModel.getOrCreateChannelAndPost(
                                        channelName = postChannelNameInput,
                                        content = postTextInput.takeIf { it.isNotBlank() } ?: "Shared a media upload",
                                        mediaUrl = attachedFileUrl,
                                        mediaType = attachedMediaType,
                                        fileName = attachedFileName,
                                        fileSize = attachedFileSize
                                    )
                                }
                                
                                showCreatePostModal = false
                                postTextInput = ""
                                attachedFileUrl = ""
                                attachedMediaType = "TEXT"
                                attachedFileName = ""
                                attachedFileSize = ""
                                postChannelNameInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Text("Post to Feed", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCreatePostModal = false
                            postTextInput = ""
                            attachedFileUrl = ""
                            attachedMediaType = "TEXT"
                            attachedFileName = ""
                            attachedFileSize = ""
                            postChannelNameInput = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PostCard(post: PostEntity, onLikeToggle: () -> Unit) {
    val formattedTime = remember(post.timestamp) {
        val sdf = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
        sdf.format(Date(post.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Post Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulseAvatar(
                    imageUrl = post.userAvatar,
                    name = post.userName,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = { /* Actions */ }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Text Content
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Post Media Content
            if (post.mediaUrl.isNotEmpty()) {
                if (post.mediaType == "IMAGE") {
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = "Post image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (post.mediaType == "VIDEO") {
                    VideoPlayer(videoUrl = post.mediaUrl, modifier = Modifier.fillMaxWidth().height(180.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (post.mediaType == "GAME") {
                    var showGameDialog by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = PulseGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = if (post.mediaUrl == "tic_tac_toe") "Classic Tic-Tac-Toe" else "Pulse Tap Speedrun ⚡",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Challenge published by ${post.userName}! Click play below to test your skills.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showGameDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PulseGreen),
                                modifier = Modifier.fillMaxWidth().testTag("play_game_btn_${post.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play Game 🎮", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (showGameDialog) {
                        AlertDialog(
                            onDismissRequest = { showGameDialog = false },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (post.mediaUrl == "tic_tac_toe") "Tic-Tac-Toe Duel" else "Pulse Tap Speedrun",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    IconButton(onClick = { showGameDialog = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Game")
                                    }
                                }
                            },
                            text = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                ) {
                                    if (post.mediaUrl == "tic_tac_toe") {
                                        TicTacToeGame()
                                    } else {
                                        PulseTapGame()
                                    }
                                }
                            },
                            confirmButton = {}
                        )
                    }
                } else if (post.mediaType == "DOCUMENT") {
                    val fileExtension = post.fileExtension.lowercase()
                    val isApk = fileExtension == "apk" || post.mediaUrl.endsWith(".apk")
                    val isZip = fileExtension == "zip" || fileExtension == "rar" || post.mediaUrl.endsWith(".zip") || post.mediaUrl.endsWith(".rar")

                    val (icon, tint, typeLabel) = when {
                        isApk -> Triple(Icons.Default.Android, PulseGreen, "Android App Package (APK)")
                        isZip -> Triple(Icons.Default.Folder, VLinkCyan, "Compressed ZIP Archive")
                        else -> Triple(Icons.Default.InsertDriveFile, PulseGreen, "Document / File")
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(tint.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        post.mediaUrl.substringAfterLast("/"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        post.fileSize.ifBlank { "2.5 MB" } + " • " + typeLabel,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { /* Simulate download */ }) {
                                Icon(Icons.Default.Download, contentDescription = "Download attachment", tint = tint)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Post Interaction Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Like Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onLikeToggle() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLikedByMe) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = post.likesCount.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (post.isLikedByMe) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Comment Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { /* Comment action */ }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "8",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share Button
                IconButton(onClick = { /* Share */ }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse(videoUrl))
                    val controller = MediaController(ctx)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun TicTacToeGame() {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var isPlayerTurn by remember { mutableStateOf(true) }
    var winner by remember { mutableStateOf<String?>(null) }
    var playerScore by remember { mutableStateOf(0) }
    var aiScore by remember { mutableStateOf(0) }

    fun checkWinner(b: List<String>): String? {
        val ways = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (way in ways) {
            if (b[way[0]].isNotEmpty() && b[way[0]] == b[way[1]] && b[way[0]] == b[way[2]]) {
                return b[way[0]]
            }
        }
        if (b.none { it.isEmpty() }) return "Draw"
        return null
    }

    LaunchedEffect(isPlayerTurn, winner) {
        if (!isPlayerTurn && winner == null) {
            delay(600)
            val emptyIndices = board.indices.filter { board[it].isEmpty() }
            if (emptyIndices.isNotEmpty()) {
                val aiMove = emptyIndices.random()
                val newBoard = board.toMutableList().apply { this[aiMove] = "O" }
                board = newBoard
                val w = checkWinner(newBoard)
                if (w != null) {
                    winner = w
                    if (w == "O") aiScore += 1
                } else {
                    isPlayerTurn = true
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You (X)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(playerScore.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = PulseGreen)
            }
            Text("VS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AI (O)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(aiScore.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Red)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0..2) {
                        val index = row * 3 + col
                        val cellText = board[index]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .clickable(enabled = cellText.isEmpty() && isPlayerTurn && winner == null) {
                                    val newBoard = board.toMutableList().apply { this[index] = "X" }
                                    board = newBoard
                                    val w = checkWinner(newBoard)
                                    if (w != null) {
                                        winner = w
                                        if (w == "X") playerScore += 1
                                    } else {
                                        isPlayerTurn = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cellText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (cellText == "X") PulseGreen else Color.Red
                            )
                        }
                    }
                }
            }
        }

        if (winner != null) {
            val announcement = when (winner) {
                "X" -> "You Win! 🎉"
                "O" -> "AI Wins! 🤖"
                else -> "It's a Draw! 🤝"
            }
            Text(announcement, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PulseGreen)
            Button(
                onClick = {
                    board = List(9) { "" }
                    winner = null
                    isPlayerTurn = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
            ) {
                Text("Play Again")
            }
        } else {
            Text(
                text = if (isPlayerTurn) "Your Turn (X)" else "AI is thinking (O)...",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PulseTapGame() {
    var count by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(10) }
    var isRunning by remember { mutableStateOf(false) }
    var highscore by remember { mutableStateOf(0) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft -= 1
            }
            isRunning = false
            if (count > highscore) highscore = count
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("High Score: $highscore", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseGreen)
            Text("Time Left: ${timeLeft}s", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (timeLeft < 4) Color.Red else MaterialTheme.colorScheme.onSurface)
        }

        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(if (isRunning) PulseGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, if (isRunning) PulseGreen else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable {
                    if (!isRunning && timeLeft == 10) {
                        isRunning = true
                    }
                    if (isRunning) {
                        count += 1
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isRunning && timeLeft == 10) {
                    Text("TAP TO START", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PulseGreen)
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = PulseGreen)
                } else if (isRunning) {
                    Text(count.toString(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = PulseGreen)
                    Text("TAPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("FINISHED!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Red)
                    Text("$count Taps", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        if (!isRunning && timeLeft < 10) {
            Button(
                onClick = {
                    count = 0
                    timeLeft = 10
                    isRunning = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
            ) {
                Text("Restart Game")
            }
        } else if (isRunning) {
            Text("Tap as fast as you can! ⚡", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Can you score over 50 taps?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
