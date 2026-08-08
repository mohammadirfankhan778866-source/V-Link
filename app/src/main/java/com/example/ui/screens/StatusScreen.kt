package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.models.ChannelEntity
import com.example.data.models.ChannelMessageEntity
import com.example.ui.components.PulseAvatar
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
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
    val channels by viewModel.channels.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeStatusViewer by viewModel.activeStatusViewer.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Status, 1 = Channels
    var showPostStatusModal by remember { mutableStateOf(false) }
    var captionInput by remember { mutableStateOf("") }
    var statusMediaUrl by remember { mutableStateOf("") }

    // Channel specific states
    var showCreateChannelModal by remember { mutableStateOf(false) }
    var channelNameInput by remember { mutableStateOf("") }
    var channelDescInput by remember { mutableStateOf("") }
    var channelAvatarInput by remember { mutableStateOf("") }
    var activeChannelDetail by remember { mutableStateOf<ChannelEntity?>(null) }
    var channelSearchQuery by remember { mutableStateOf("") }

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

    val channelCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = MediaUtils.copyUriToInternalStorage(context, uri, "channel_covers")
            if (localPath != null) {
                channelAvatarInput = "file://$localPath"
            }
        }
    }

    val unviewedStatuses = remember(statuses) { statuses.filter { !it.isViewed && !it.isMine } }
    val viewedStatuses = remember(statuses) { statuses.filter { it.isViewed && !it.isMine } }
    val myStatus = remember(statuses) { statuses.firstOrNull { it.isMine } }

    val currentUserId = currentUser?.id ?: "usr_google_irfan_9075"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Updates", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = { showPostStatusModal = true },
                    containerColor = PulseGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("post_status_fab")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Add Status")
                }
            } else {
                FloatingActionButton(
                    onClick = { showCreateChannelModal = true },
                    containerColor = PulseGreen,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_channel_fab")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = "Create Channel")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    label = { Text("Status Stories", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.DonutLarge, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PulseGreen.copy(alpha = 0.15f),
                        selectedLabelColor = PulseGreen,
                        selectedLeadingIconColor = PulseGreen
                    )
                )

                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    label = { Text("Public Channels", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PulseGreen.copy(alpha = 0.15f),
                        selectedLabelColor = PulseGreen,
                        selectedLeadingIconColor = PulseGreen
                    )
                )
            }

            if (activeTab == 0) {
                // STATUS TAB UI
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
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
            } else {
                // CHANNELS TAB UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = channelSearchQuery,
                        onValueChange = { channelSearchQuery = it },
                        placeholder = { Text("Search public channels...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(24.dp)
                    )

                    val filteredChannels = remember(channels, channelSearchQuery) {
                        channels.filter { it.name.contains(channelSearchQuery, ignoreCase = true) || it.description.contains(channelSearchQuery, ignoreCase = true) }
                    }

                    val followedChannels = filteredChannels.filter { it.isFollowedByMe || it.creatorId == currentUserId }
                    val discoverChannels = filteredChannels.filter { !it.isFollowedByMe && it.creatorId != currentUserId }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section: My Channels / Followed
                        if (followedChannels.isNotEmpty()) {
                            item {
                                Text("CHANNELS YOU FOLLOW", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PulseGreen)
                            }
                            items(followedChannels, key = { it.id }) { channel ->
                                ChannelRowItem(
                                    channel = channel,
                                    onClick = { activeChannelDetail = channel },
                                    onFollowToggle = { viewModel.toggleFollowChannel(channel) }
                                )
                            }
                        }

                        // Section: Discover Channels
                        if (discoverChannels.isNotEmpty()) {
                            item {
                                Text("DISCOVER PUBLIC CHANNELS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                            }
                            items(discoverChannels, key = { it.id }) { channel ->
                                ChannelRowItem(
                                    channel = channel,
                                    onClick = { activeChannelDetail = channel },
                                    onFollowToggle = { viewModel.toggleFollowChannel(channel) }
                                )
                            }
                        }

                        if (filteredChannels.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No channels found", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
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

        // Create Channel Modal
        if (showCreateChannelModal) {
            AlertDialog(
                onDismissRequest = { showCreateChannelModal = false },
                title = { Text("Create Public Channel", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                PulseAvatar(
                                    imageUrl = channelAvatarInput.ifBlank { "https://picsum.photos/seed/placeholder/300/300" },
                                    name = channelNameInput.ifBlank { "New" },
                                    size = 80.dp
                                )
                                IconButton(
                                    onClick = {
                                        channelAvatarInput = "https://picsum.photos/seed/chan_${System.currentTimeMillis()}/300/300"
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(PulseGreen, CircleShape)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Randomize", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = channelNameInput,
                            onValueChange = { channelNameInput = it },
                            label = { Text("Channel Name") },
                            leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = PulseGreen) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = channelDescInput,
                            onValueChange = { channelDescInput = it },
                            label = { Text("Description / Rules") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = PulseGreen) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                channelCoverPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = PulseGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Cover Photo", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (channelNameInput.isNotBlank()) {
                                viewModel.createChannel(
                                    name = channelNameInput,
                                    description = channelDescInput,
                                    avatarUrl = channelAvatarInput
                                )
                                showCreateChannelModal = false
                                channelNameInput = ""
                                channelDescInput = ""
                                channelAvatarInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Text("Create Channel", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateChannelModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Channel Detailed Feed / Chat Overlay
        if (activeChannelDetail != null) {
            val channel = activeChannelDetail!!
            val channelMessages by viewModel.getMessagesForChannel(channel.id).collectAsState(initial = emptyList())
            var broadcastInput by remember { mutableStateOf("") }
            var attachedFileUrlChannel by remember { mutableStateOf("") }
            var attachedFileTypeChannel by remember { mutableStateOf("TEXT") }

            val isAdmin = channel.creatorId == currentUserId

            Dialog(onDismissRequest = { activeChannelDetail = null }) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Chat Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { activeChannelDetail = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }

                            PulseAvatar(imageUrl = channel.avatarUrl, name = channel.name, size = 42.dp)

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                Text("${channel.followerCount} followers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = {
                                    viewModel.toggleFollowChannel(channel)
                                    // Live updates the current overlay state
                                    activeChannelDetail = channel.copy(
                                        isFollowedByMe = !channel.isFollowedByMe,
                                        followerCount = channel.followerCount + (if (channel.isFollowedByMe) -1 else 1)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (channel.isFollowedByMe) MaterialTheme.colorScheme.surfaceVariant else PulseGreen
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (channel.isFollowedByMe) "Unfollow" else "Follow",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (channel.isFollowedByMe) MaterialTheme.colorScheme.onSurface else Color.White
                                )
                            }
                        }

                        // Description Box
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("About this Channel", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PulseGreen)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(channel.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Message Feed
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(channelMessages) { msg ->
                                ChannelMessageCard(msg = msg)
                            }

                            if (channelMessages.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No announcements yet. Updates posted by admins will appear here.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Input Bar
                        if (isAdmin) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        // Quick seed image broadcast attachment
                                        attachedFileUrlChannel = "https://picsum.photos/seed/announcement_${System.currentTimeMillis()}/800/600"
                                        attachedFileTypeChannel = "IMAGE"
                                    }
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = "Attach image", tint = PulseGreen)
                                }

                                IconButton(
                                    onClick = {
                                        attachedFileUrlChannel = "file://documents/update_brief.pdf"
                                        attachedFileTypeChannel = "DOCUMENT"
                                    }
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file", tint = VLinkCyan)
                                }

                                OutlinedTextField(
                                    value = broadcastInput,
                                    onValueChange = { broadcastInput = it },
                                    placeholder = { Text("Broadcast an update...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 3
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = {
                                        if (broadcastInput.isNotBlank() || attachedFileUrlChannel.isNotEmpty()) {
                                            viewModel.sendChannelMessage(
                                                channelId = channel.id,
                                                content = broadcastInput,
                                                mediaUrl = attachedFileUrlChannel,
                                                mediaType = attachedFileTypeChannel,
                                                fileName = if (attachedFileTypeChannel == "DOCUMENT") "update_brief.pdf" else "broadcast.jpg",
                                                fileSize = if (attachedFileTypeChannel == "DOCUMENT") "2.8 MB" else "1.1 MB"
                                            )
                                            broadcastInput = ""
                                            attachedFileUrlChannel = ""
                                            attachedFileTypeChannel = "TEXT"
                                        }
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PulseGreen)
                                }
                            }

                            // Active Attachment Tag
                            if (attachedFileUrlChannel.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PulseGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Media File ready to broadcast", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(onClick = {
                                        attachedFileUrlChannel = ""
                                        attachedFileTypeChannel = "TEXT"
                                    }) {
                                        Text("Cancel", fontSize = 11.sp, color = Color.Red)
                                    }
                                }
                            }
                        } else {
                            // Read-only Banner for followers
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Only administrators can post updates to this channel.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelMessageCard(msg: ChannelMessageEntity) {
    val formattedTime = remember(msg.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(msg.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulseAvatar(imageUrl = msg.senderAvatar, name = msg.senderName, size = 24.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(msg.senderName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PulseGreen)
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(PulseGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ADMIN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PulseGreen)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Media Preview
            if (msg.mediaUrl.isNotEmpty()) {
                if (msg.mediaType == "IMAGE") {
                    AsyncImage(
                        model = msg.mediaUrl,
                        contentDescription = "Broadcast photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (msg.mediaType == "DOCUMENT") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = PulseGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(msg.fileName.ifBlank { "document.pdf" }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(msg.fileSize.ifBlank { "2.4 MB" } + " • PDF", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { }) {
                                Icon(Icons.Default.Download, contentDescription = "Download", tint = PulseGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Message Content
            if (msg.content.isNotBlank()) {
                Text(msg.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Timestamp Footer
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                Text(formattedTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun ChannelRowItem(channel: ChannelEntity, onClick: () -> Unit, onFollowToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulseAvatar(imageUrl = channel.avatarUrl, name = channel.name, size = 52.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                text = if (channel.lastMessageText.isNotBlank()) channel.lastMessageText else channel.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("${channel.followerCount} followers", fontSize = 10.sp, color = PulseGreen, fontWeight = FontWeight.SemiBold)
        }

        IconButton(onClick = onFollowToggle) {
            Icon(
                imageVector = if (channel.isFollowedByMe) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                contentDescription = if (channel.isFollowedByMe) "Following" else "Follow",
                tint = if (channel.isFollowedByMe) PulseGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun StatusRowItem(status: StatusStoryEntity, onClick: () -> Unit) {
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
            size = 52.dp,
            hasStory = !status.isViewed
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(status.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                text = if (status.caption.isNotBlank()) status.caption else "Tap to view status update",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

