package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PulseAvatar(
    imageUrl: String,
    name: String,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    hasStory: Boolean = false,
    storySeen: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val storyBorderModifier = if (hasStory) {
        val borderColor = if (storySeen) Color.Gray else PulseGreen
        Modifier.border(2.dp, borderColor, CircleShape).padding(2.dp)
    } else Modifier

    Box(
        modifier = Modifier
            .size(size)
            .then(storyBorderModifier)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = name.firstOrNull()?.uppercase() ?: "P"
            Text(
                text = initial,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4).sp
            )
        }

        if (isOnline) {
            val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            val auraAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "auraAlpha"
            )

            // Outer breathing aura ring
            Box(
                modifier = Modifier
                    .size(size * 0.38f * pulseScale)
                    .clip(CircleShape)
                    .background(OnlineGreen.copy(alpha = auraAlpha))
                    .align(Alignment.BottomEnd)
            )

            // Inner solid status dot
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun RealtimeConnectivityBadge(
    isConnected: Boolean,
    onlineStatus: String = "ONLINE",
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val (statusColor, statusText) = when {
        !isConnected -> Color(0xFFE53935) to "Disconnected"
        onlineStatus == "AWAY" -> Color(0xFFFFB300) to "Away"
        onlineStatus == "OFFLINE" -> Color.Gray to "Invisible"
        else -> OnlineGreen to "Online"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = statusColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isConnected && onlineStatus == "ONLINE") statusColor.copy(alpha = alpha) else statusColor)
            )
            if (showLabel) {
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun MessageStatusTicks(status: String) {
    when (status) {
        MessageStatus.PENDING.name -> {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = "Pending",
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
        }
        MessageStatus.SENT.name -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = DeliveredTickGray,
                modifier = Modifier.size(14.dp)
            )
        }
        MessageStatus.DELIVERED.name -> {
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                Icon(Icons.Default.Check, "Delivered 1", tint = DeliveredTickGray, modifier = Modifier.size(14.dp))
                Icon(Icons.Default.Check, "Delivered 2", tint = DeliveredTickGray, modifier = Modifier.size(14.dp))
            }
        }
        MessageStatus.READ.name -> {
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                Icon(Icons.Default.Check, "Read 1", tint = ReadTickBlue, modifier = Modifier.size(14.dp))
                Icon(Icons.Default.Check, "Read 2", tint = ReadTickBlue, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun VoiceNotePlayer(
    duration: String = "0:14",
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_waveform")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (Math.PI.toFloat() * 2f) else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.08f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .size(36.dp)
                .background(VoiceMicAccent, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play voice note",
                tint = Color.White
            )
        }

        // Live Animated Waveform Canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
        ) {
            val bars = 26
            val barWidth = size.width / (bars * 1.5f)
            val baseHeights = listOf(0.3f, 0.6f, 0.9f, 0.4f, 0.8f, 0.5f, 0.2f, 0.7f, 1.0f, 0.6f, 0.3f, 0.8f, 0.5f, 0.9f, 0.4f, 0.7f, 0.3f, 0.6f, 0.8f, 0.4f, 0.2f, 0.5f, 0.7f, 0.3f, 0.6f, 0.4f)
            for (i in 0 until bars) {
                val x = i * (barWidth * 1.5f)
                val base = baseHeights[i % baseHeights.size]
                val dynamicMultiplier = if (isPlaying) {
                    0.6f + 0.4f * kotlin.math.sin(wavePhase + i * 0.4f).toFloat()
                } else 1.0f
                val h = (size.height * base * dynamicMultiplier).coerceIn(4f, size.height)
                val y = (size.height - h) / 2
                val color = if (i < (bars * if (isPlaying) 0.6f else 0.2f)) VoiceMicAccent else Color.Gray.copy(alpha = 0.5f)
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        }

        Text(
            text = duration,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Animated Typing status display component that renders bouncing dots in a smooth sine wave and a typing label.
 */
@Composable
fun TypingStatusDisplay(
    userName: String = "Typing",
    modifier: Modifier = Modifier,
    showAvatar: Boolean = false,
    avatarUrl: String = ""
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier
            .testTag("typing_status_display")
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showAvatar) {
                PulseAvatar(
                    imageUrl = avatarUrl,
                    name = userName,
                    size = 20.dp
                )
            }
            Text(
                text = if (userName == "Typing") "typing..." else "$userName is typing...",
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = PulseGreen,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .offset(y = dot1Offset.dp)
                        .clip(CircleShape)
                        .background(PulseGreen)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .offset(y = dot2Offset.dp)
                        .clip(CircleShape)
                        .background(PulseGreen)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .offset(y = dot3Offset.dp)
                        .clip(CircleShape)
                        .background(PulseGreen)
                )
            }
        }
    }
}

/**
 * Live animated soundwave equalizer bars rendered during active voice note recording.
 */
@Composable
fun LiveRecordingSoundWave(
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFFE53935)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording_bars")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.height(20.dp)) {
        val bars = 18
        val spacing = 4.dp.toPx()
        val barWidth = (size.width - (bars - 1) * spacing) / bars
        for (i in 0 until bars) {
            val sinVal = kotlin.math.abs(kotlin.math.sin(phase + i * 0.45f)).toFloat()
            val barHeight = ((size.height * 0.25f) + (size.height * 0.75f * sinVal)).coerceIn(4f, size.height)
            val x = i * (barWidth + spacing)
            val y = (size.height - barHeight) / 2
            drawRoundRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }
}

/**
 * Sonar pulse ripple rings radiating around active call avatars or active recording buttons.
 */
@Composable
fun SonarPulseRipple(
    targetSize: Dp = 100.dp,
    rippleColor: Color = VLinkCyan,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sonar_ripple")
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 0, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 0, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )

    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Scale"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Alpha"
    )

    Box(modifier = modifier.size(targetSize), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(targetSize * ring1Scale)
                .clip(CircleShape)
                .background(rippleColor.copy(alpha = ring1Alpha))
        )
        Box(
            modifier = Modifier
                .size(targetSize * ring2Scale)
                .clip(CircleShape)
                .background(rippleColor.copy(alpha = ring2Alpha))
        )
    }
}

/**
 * Confirmation dialog for safely deleting sent/received messages.
 */
@Composable
fun DeleteMessageConfirmationDialog(
    isOutgoing: Boolean,
    onDismiss: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text("Delete message?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Text(
                text = if (isOutgoing) {
                    "Are you sure you want to delete this message? You can delete it just for yourself or for everyone in the chat."
                } else {
                    "Are you sure you want to delete this message from your chat history?"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                if (isOutgoing && onDeleteForEveryone != null) {
                    TextButton(
                        onClick = {
                            onDeleteForEveryone()
                            onDismiss()
                        }
                    ) {
                        Text(
                            "Delete for Everyone",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(
                    onClick = {
                        onDeleteForMe()
                        onDismiss()
                    }
                ) {
                    Text(
                        "Delete for Me",
                        color = if (isOutgoing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        fontWeight = if (!isOutgoing) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Reusable icon item for attachment picker sheet.
 */
@Composable
fun AttachmentOptionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FilterChipGroup(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("All", "Unread", "Groups")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
