package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.models.MessageEntity
import com.example.data.models.MessageType
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMediaGalleryBottomSheet(
    messages: List<MessageEntity>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var viewingMediaMessage by remember { mutableStateOf<MessageEntity?>(null) }

    val mediaMessages = remember(messages) {
        messages.filter { 
            it.type == MessageType.IMAGE.name || 
            it.type == MessageType.VIDEO.name || 
            (it.mediaUrl.isNotBlank() && it.type != MessageType.DOCUMENT.name && it.type != MessageType.VOICE_NOTE.name)
        }
    }

    val docMessages = remember(messages) {
        messages.filter { 
            it.type == MessageType.DOCUMENT.name || 
            it.type == MessageType.VOICE_NOTE.name ||
            it.fileName.isNotBlank()
        }
    }

    val linkMessages = remember(messages) {
        messages.filter { 
            it.content.contains("http://", ignoreCase = true) || 
            it.content.contains("https://", ignoreCase = true) 
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("shared_media_gallery_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PermMedia,
                        contentDescription = null,
                        tint = PulseGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shared Media & Files",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            "Media (${mediaMessages.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            "Docs (${docMessages.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { 
                        Text(
                            "Links (${linkMessages.size})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        if (mediaMessages.isEmpty()) {
                            EmptyGalleryPlaceholder(
                                icon = Icons.Outlined.PhotoLibrary,
                                title = "No Media Shared Yet",
                                subtitle = "Photos and videos sent in this chat will appear here."
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(mediaMessages, key = { it.id }) { msg ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewingMediaMessage = msg }
                                    ) {
                                        AsyncImage(
                                            model = msg.mediaUrl,
                                            contentDescription = msg.content,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (msg.type == MessageType.VIDEO.name) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayCircleFilled,
                                                    contentDescription = "Video",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (docMessages.isEmpty()) {
                            EmptyGalleryPlaceholder(
                                icon = Icons.Outlined.Description,
                                title = "No Documents Shared",
                                subtitle = "PDFs, files and audio recordings sent will be listed here."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(docMessages, key = { it.id }) { msg ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (msg.type == MessageType.VOICE_NOTE.name) Icons.Default.Mic else Icons.Default.InsertDriveFile,
                                                contentDescription = null,
                                                tint = PulseGreen,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = msg.fileName.ifBlank { msg.content },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${msg.senderName} • ${msg.fileSize.ifBlank { "Attachment" }}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        if (linkMessages.isEmpty()) {
                            EmptyGalleryPlaceholder(
                                icon = Icons.Outlined.Link,
                                title = "No Links Shared",
                                subtitle = "Web links and URLs sent in this conversation will be cataloged here."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(linkMessages, key = { it.id }) { msg ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Language,
                                                contentDescription = null,
                                                tint = VLinkCyan,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = msg.content,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = PulseGreen
                                                )
                                                Text(
                                                    text = "Sent by ${msg.senderName}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }

    // Fullscreen High-Res Media Viewer
    viewingMediaMessage?.let { msg ->
        Dialog(
            onDismissRequest = { viewingMediaMessage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = msg.mediaUrl,
                        contentDescription = msg.content,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Top Bar Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewingMediaMessage = null },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }

                        Text(
                            text = "Shared by ${msg.senderName}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Bottom Caption
                    if (msg.content.isNotBlank() && msg.content != "Photo" && msg.content != "Video") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = msg.content,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyGalleryPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
