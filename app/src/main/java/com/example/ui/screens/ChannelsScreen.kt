package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.models.ChannelEntity
import com.example.data.models.ChannelMessageEntity
import com.example.data.models.UserEntity
import com.example.ui.components.PulseAvatar
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
import com.example.ui.viewmodels.MainViewModel
import com.example.util.MediaUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(viewModel: MainViewModel) {
    val channels by viewModel.channels.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    var showCreateChannelModal by remember { mutableStateOf(false) }
    var channelNameInput by remember { mutableStateOf("") }
    var channelDescInput by remember { mutableStateOf("") }
    var channelAvatarInput by remember { mutableStateOf("") }
    var channelVisibility by remember { mutableStateOf("PUBLIC") }
    var activeChannelDetail by remember { mutableStateOf<ChannelEntity?>(null) }
    var channelSearchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current
    val currentUserId = currentUser?.id ?: "usr_google_irfan_9075"

    val channelCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = MediaUtils.copyUriToInternalStorage(context, uri, "channel_covers")
            if (localPath != null) {
                channelAvatarInput = "file://\$localPath"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channels", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateChannelModal = true },
                containerColor = PulseGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_channel_fab")
            ) {
                Icon(Icons.Default.Campaign, contentDescription = "Create Channel")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = channelSearchQuery,
                onValueChange = { channelSearchQuery = it },
                placeholder = { Text("Search public channels...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp)
            )

            val filteredChannels = remember(channels, channelSearchQuery) {
                channels.filter { it.name.contains(channelSearchQuery, ignoreCase = true) || it.description.contains(channelSearchQuery, ignoreCase = true) }
            }
            val followedChannels = filteredChannels.filter { it.isFollowedByMe || it.creatorId == currentUserId }
            val discoverChannels = filteredChannels.filter { !it.isFollowedByMe && it.creatorId != currentUserId && it.visibility == "PUBLIC" }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                if (discoverChannels.isNotEmpty()) {
                    item {
                        Text("DISCOVER CHANNELS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(discoverChannels, key = { it.id }) { channel ->
                        ChannelRowItem(
                            channel = channel,
                            onClick = { activeChannelDetail = channel },
                            onFollowToggle = { viewModel.toggleFollowChannel(channel) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateChannelModal) {
        AlertDialog(
            onDismissRequest = { showCreateChannelModal = false },
            title = { Text("Create Channel") },
            text = {
                Column {
                    if (channelAvatarInput.isNotEmpty()) {
                        AsyncImage(
                            model = channelAvatarInput,
                            contentDescription = "Channel Cover Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        OutlinedButton(
                            onClick = { 
                                channelCoverPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Channel Cover")
                        }
                    }

                    OutlinedTextField(
                        value = channelNameInput,
                        onValueChange = { channelNameInput = it },
                        label = { Text("Channel Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Visibility Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Visibility: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = channelVisibility == "PUBLIC",
                            onClick = { channelVisibility = "PUBLIC" },
                            label = { Text("Public") },
                            leadingIcon = { if (channelVisibility == "PUBLIC") Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = channelVisibility == "FRIENDS_ONLY",
                            onClick = { channelVisibility = "FRIENDS_ONLY" },
                            label = { Text("Private / Friends Only") },
                            leadingIcon = { if (channelVisibility == "FRIENDS_ONLY") Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
OutlinedTextField(
                        value = channelDescInput,
                        onValueChange = { channelDescInput = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (channelNameInput.isNotBlank()) {
                            viewModel.createChannel(
                                name = channelNameInput.trim(),
                                description = channelDescInput.trim(),
                                avatarUrl = channelAvatarInput.ifBlank { "https://picsum.photos/seed/${channelNameInput.trim()}/300/300" },
                                visibility = channelVisibility
                            )
                            showCreateChannelModal = false
                            channelNameInput = ""
                            channelDescInput = ""
                            channelAvatarInput = ""
                            channelVisibility = "PUBLIC"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseGreen),
                    enabled = channelNameInput.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateChannelModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (activeChannelDetail != null) {
        Dialog(onDismissRequest = { activeChannelDetail = null }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            val channel = activeChannelDetail!!
            val messages by viewModel.getMessagesForChannel(channel.id).collectAsState(initial = emptyList())
            var channelMsgInput by remember { mutableStateOf("") }
            val isOwner = channel.creatorId == currentUserId

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulseAvatar(imageUrl = channel.avatarUrl, name = channel.name, size = 36.dp, isOnline = false)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("\${channel.followerCount} followers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { activeChannelDetail = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (!isOwner) {
                                Button(
                                    onClick = { 
                                        viewModel.toggleFollowChannel(channel)
                                        activeChannelDetail = channel.copy(
                                            isFollowedByMe = !channel.isFollowedByMe,
                                            followerCount = if (channel.isFollowedByMe) channel.followerCount - 1 else channel.followerCount + 1
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (channel.isFollowedByMe) Color.Transparent else PulseGreen),
                                    modifier = if (channel.isFollowedByMe) Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(20.dp)) else Modifier,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(if (channel.isFollowedByMe) "Following" else "Follow", color = if (channel.isFollowedByMe) MaterialTheme.colorScheme.onSurface else Color.White)
                                }
                            }
                        }
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        reverseLayout = true,
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChannelMessageCard(msg = msg)
                        }
                        item {
                            Text(
                                text = channel.description,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    if (isOwner) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = channelMsgInput,
                                    onValueChange = { channelMsgInput = it },
                                    placeholder = { Text("Broadcast to followers...") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (channelMsgInput.isNotBlank()) {
                                            viewModel.sendChannelMessage(channel.id, channelMsgInput)
                                            channelMsgInput = ""
                                        }
                                    },
                                    modifier = Modifier.background(PulseGreen, CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                                }
                            }
                        }
                    } else if (!channel.isFollowedByMe) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .navigationBarsPadding()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Follow this channel to receive updates.")
                        }
                    }
                }
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
        PulseAvatar(
            imageUrl = channel.avatarUrl,
            name = channel.name,
            size = 56.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("\${channel.followerCount} followers", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = onFollowToggle,
            colors = ButtonDefaults.buttonColors(containerColor = if (channel.isFollowedByMe) Color.Transparent else PulseGreen),
            modifier = if (channel.isFollowedByMe) Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(20.dp)) else Modifier,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(if (channel.isFollowedByMe) "Following" else "Follow", color = if (channel.isFollowedByMe) MaterialTheme.colorScheme.onSurface else Color.White)
        }
    }
}

@Composable
fun ChannelMessageCard(msg: ChannelMessageEntity) {
    val formattedTime = remember(msg.timestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(msg.timestamp))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (msg.mediaUrl != null) {
                AsyncImage(
                    model = msg.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)).padding(bottom = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Text(msg.content, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(formattedTime, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
        }
    }
}
