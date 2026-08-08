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

        // Waveform Canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
        ) {
            val bars = 24
            val barWidth = size.width / (bars * 1.5f)
            val heights = listOf(0.3f, 0.6f, 0.9f, 0.4f, 0.8f, 0.5f, 0.2f, 0.7f, 1.0f, 0.6f, 0.3f, 0.8f, 0.5f, 0.9f, 0.4f, 0.7f, 0.3f, 0.6f, 0.8f, 0.4f, 0.2f, 0.5f, 0.7f, 0.3f)
            for (i in 0 until bars) {
                val x = i * (barWidth * 1.5f)
                val h = size.height * (heights[i % heights.size])
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
