package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.models.*
import com.example.ui.theme.CallEndRed
import com.example.ui.theme.VLinkCyan
import com.example.ui.theme.VLinkViolet
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CallActiveOverlay(
    call: CallLogEntity,
    viewModel: com.example.ui.viewmodels.MainViewModel,
    onEndCall: () -> Unit
) {
    val isGroupCall by viewModel.isGroupCall.collectAsState()
    val groupParticipants by viewModel.groupParticipants.collectAsState()
    val isWeakNetworkSimulated by viewModel.isWeakNetworkSimulated.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    var isMuted by remember { mutableStateOf(false) }
    var isVideoOn by remember { mutableStateOf(call.callType == CallType.VIDEO.name) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var isMinimized by remember { mutableStateOf(false) }
    var showInCallChat by remember { mutableStateOf(false) }
    var inCallMessageInput by remember { mutableStateOf("") }
    val inCallMessages = remember { mutableStateListOf("WebRTC Multi-Peer Mesh Session established!") }

    var secondsElapsed by remember { mutableIntStateOf(0) }
    var webRtcStatus by remember { mutableStateOf("Initializing WebRTC Media Engine...") }
    var isConnected by remember { mutableStateOf(false) }

    // Dialog state for inviting a new participant
    var showInviteDialog by remember { mutableStateOf(false) }
    
    // Dialog state for participant individual controls (mute, volume, stats)
    var selectedParticipantForControls by remember { mutableStateOf<CallParticipant?>(null) }

    // Simulated WebRTC Peer Connection Lifecycle
    LaunchedEffect(Unit) {
        delay(400)
        webRtcStatus = "Creating SDP Offer & Gathering ICE Candidates..."
        delay(600)
        webRtcStatus = "Exchanging WebRTC Peer Keys..."
        delay(500)
        webRtcStatus = "Multi-Peer Connected • 1080p Opus HD"
        isConnected = true

        while (true) {
            delay(1000)
            secondsElapsed++
        }
    }

    val minutes = secondsElapsed / 60
    val secs = secondsElapsed % 60
    val formattedDuration = String.format("%02d:%02d", minutes, secs)

    // Camera rotation animation for front/back flip
    val cameraRotation by animateFloatAsState(
        targetValue = if (isFrontCamera) 0f else 180f,
        animationSpec = tween(350),
        label = "cameraRotation"
    )

    if (isMinimized) {
        // Floating Draggable Picture-in-Picture Call Bubble
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(width = 150.dp, height = 210.dp)
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .shadow(16.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, VLinkCyan, RoundedCornerShape(20.dp))
                    .clickable { isMinimized = false }
                    .testTag("pip_call_bubble"),
                contentAlignment = Alignment.Center
            ) {
                if (isVideoOn) {
                    AsyncImage(
                        model = if (isFrontCamera) "https://picsum.photos/seed/vlinkcall/400/600" else "https://picsum.photos/seed/backcam/400/600",
                        contentDescription = "Video Stream",
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(cameraRotation),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        PulseAvatar(imageUrl = call.contactAvatar, name = call.contactName, size = 60.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(call.contactName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(
                            text = if (isGroupCall) "Group Call" else formattedDuration,
                            color = VLinkCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Expand button overlay
                IconButton(
                    onClick = { isMinimized = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.OpenInFull, contentDescription = "Expand Call", tint = Color.White, modifier = Modifier.size(16.dp))
                }

                // Quick end call in PiP
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(CallEndRed, CircleShape)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    } else {
        // Fullscreen Interactive WebRTC Call Dialog
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF090D16)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    
                    // Main Video Feed / Call Grid Layout
                    if (isGroupCall) {
                        // Multi-Peer Group Call Layout
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 90.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Subtitle of Call Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Connected: ${groupParticipants.count { it.connectionState == ParticipantConnectionState.CONNECTED }} / ${groupParticipants.size}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // Connection Alert Banner if weak network is active
                                if (isWeakNetworkSimulated) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(12.dp))
                                            Text("Simulating Congestion", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Dynamic Grid of streams
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Self (Current User) Stream
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .aspectRatio(0.75f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF1E293B))
                                            .border(
                                                width = 2.dp,
                                                color = if (!isMuted) VLinkCyan.copy(alpha = 0.3f) else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isVideoOn) {
                                            AsyncImage(
                                                model = "https://picsum.photos/seed/irfan_self/300/400",
                                                contentDescription = "My Camera Stream",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                PulseAvatar(
                                                    imageUrl = "https://picsum.photos/seed/irfan/300/300",
                                                    name = "Mohammad Irfan Khan",
                                                    size = 70.dp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("You (Host)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text("Video Off", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                            }
                                        }

                                        // Status Overlay
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(8.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(
                                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = if (isMuted) Color.Red else VLinkCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text("You", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Peer Connections (Active Call Participants)
                                items(groupParticipants, key = { it.id }) { participant ->
                                    val isMutedByMe = participant.isMutedByMe
                                    val connectionLabel = when (participant.connectionState) {
                                        ParticipantConnectionState.RINGING -> "Ringing..."
                                        ParticipantConnectionState.CONNECTING -> "Connecting..."
                                        ParticipantConnectionState.CONNECTED -> "Connected"
                                        ParticipantConnectionState.MUTED -> "Muted"
                                        ParticipantConnectionState.DISCONNECTED -> "Disconnected"
                                        ParticipantConnectionState.DECLINED -> "Declined"
                                        ParticipantConnectionState.INVITED -> "Invited"
                                    }

                                    // Pulse Border Animation for Active Speaker
                                    val infiniteTransition = rememberInfiniteTransition(label = "borderPulse")
                                    val borderWidth by infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = if (participant.isSpeaking && participant.connectionState == ParticipantConnectionState.CONNECTED) 3.5f else 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(600, easing = EaseInOutSine),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "borderWidth"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(0.75f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF111827))
                                            .border(
                                                width = borderWidth.dp,
                                                color = if (participant.isSpeaking && participant.connectionState == ParticipantConnectionState.CONNECTED) VLinkCyan else Color.White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable { selectedParticipantForControls = participant },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (participant.connectionState == ParticipantConnectionState.CONNECTED && participant.isVideoEnabled) {
                                            AsyncImage(
                                                model = "https://picsum.photos/seed/${participant.id}/300/400",
                                                contentDescription = "${participant.name} Stream",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            // Centered Info (Avatar + Ringing / Connection State)
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (participant.connectionState == ParticipantConnectionState.RINGING) {
                                                        val ringPulse by infiniteTransition.animateFloat(
                                                            initialValue = 60f,
                                                            targetValue = 90f,
                                                            animationSpec = infiniteRepeatable(
                                                                animation = tween(1200, easing = FastOutSlowInEasing),
                                                                repeatMode = RepeatMode.Restart
                                                            ),
                                                            label = "ringPulse"
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(ringPulse.dp)
                                                                .clip(CircleShape)
                                                                .background(VLinkCyan.copy(alpha = 0.15f))
                                                        )
                                                    }
                                                    PulseAvatar(
                                                        imageUrl = participant.avatarUrl,
                                                        name = participant.name,
                                                        size = 60.dp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    participant.name,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = connectionLabel,
                                                    color = when (participant.connectionState) {
                                                        ParticipantConnectionState.CONNECTED -> VLinkCyan
                                                        ParticipantConnectionState.RINGING -> Color(0xFFFFB74D)
                                                        ParticipantConnectionState.DECLINED, ParticipantConnectionState.DISCONNECTED -> Color.Red
                                                        else -> Color.White.copy(alpha = 0.6f)
                                                    },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }

                                        // Left Bottom Overlay Card: Name & Volume indicator
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(8.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        isMutedByMe -> Icons.Default.VolumeMute
                                                        participant.volume < 0.3f -> Icons.Default.VolumeDown
                                                        else -> Icons.Default.VolumeUp
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isMutedByMe) Color.Red else Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = participant.name.takeWhile { !it.isWhitespace() },
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        // WebRTC Connection Quality Badge (Top Left Overlay)
                                        if (participant.connectionState == ParticipantConnectionState.CONNECTED) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isWeakNetworkSimulated) Color(0xFFEF4444).copy(alpha = 0.8f)
                                                        else Color(0xFF10B981).copy(alpha = 0.8f)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.SignalCellularAlt,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = "${participant.stats.bitrateKbps}kbps",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 1-on-1 Call Video Feed Layer
                        if (isVideoOn) {
                            // Remote Participant Video Feed
                            AsyncImage(
                                model = if (isFrontCamera) "https://picsum.photos/seed/remote_video/1080/1920" else "https://picsum.photos/seed/rear_video/1080/1920",
                                contentDescription = "WebRTC Video Stream",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(cameraRotation),
                                contentScale = ContentScale.Crop
                            )

                            // Dark Gradient for readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.7f),
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            // Local Camera Feed (Self Preview Box)
                            Box(
                                modifier = Modifier
                                    .padding(top = 70.dp, end = 20.dp)
                                    .size(width = 110.dp, height = 160.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.Black.copy(alpha = 0.8f))
                                    .border(1.5.dp, VLinkCyan.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                                    .align(Alignment.TopEnd)
                            ) {
                                AsyncImage(
                                    model = "https://picsum.photos/seed/irfan/300/300",
                                    contentDescription = "Local Camera Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Flip Camera Button inside preview
                                IconButton(
                                    onClick = { isFrontCamera = !isFrontCamera },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Main Call Profile Section for 1-to-1 Call
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 110.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!isVideoOn) {
                                Box(contentAlignment = Alignment.Center) {
                                    // Pulsing Audio Ripple Effect
                                    Box(
                                        modifier = Modifier
                                            .size(160.dp)
                                            .clip(CircleShape)
                                            .background(VLinkCyan.copy(alpha = 0.15f))
                                    )
                                    PulseAvatar(
                                        imageUrl = call.contactAvatar,
                                        name = call.contactName,
                                        size = 120.dp
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            Text(
                                text = call.contactName,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (!isConnected) webRtcStatus else formattedDuration,
                                color = if (isConnected) VLinkCyan else Color(0xFFFFB74D),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = webRtcStatus,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
                            )

                            // Realtime Mic Audio Waveform Bar Visualizer (When Voice Call Active)
                            if (!isVideoOn && !isMuted) {
                                Spacer(modifier = Modifier.height(20.dp))
                                AnimatedAudioWaveform()
                            }
                            
                            // Elevated Call Badge for upgrading 1-to-1 to Group Call
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    viewModel.startGroupCall(
                                        groupTitle = "Pulse Team Sync",
                                        groupAvatar = "https://picsum.photos/seed/teamsync/300/300",
                                        isVideo = isVideoOn
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan.copy(alpha = 0.2f)),
                                modifier = Modifier.border(1.dp, VLinkCyan, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = VLinkCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Participant (P2P Mesh)", color = VLinkCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Top Bar Header Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minimize to PiP Button
                        IconButton(
                            onClick = { isMinimized = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .testTag("minimize_call_button")
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // WebRTC Network/Security badge and toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // E2EE Lock Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.dp, VLinkCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = VLinkCyan, modifier = Modifier.size(14.dp))
                                Text("E2EE WebRTC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Weak Network Simulator Switch (Only shown for Group Call testing)
                            if (isGroupCall) {
                                IconButton(
                                    onClick = { viewModel.toggleWeakNetworkSimulation() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isWeakNetworkSimulated) Color(0xFFEF4444).copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.15f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isWeakNetworkSimulated) Icons.Default.SignalCellularConnectedNoInternet0Bar else Icons.Default.NetworkCheck,
                                        contentDescription = "Simulate Network Quality Issues",
                                        tint = if (isWeakNetworkSimulated) Color(0xFFF87171) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Direct Person Inviter Button
                                IconButton(
                                    onClick = { showInviteDialog = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(VLinkCyan.copy(alpha = 0.25f), CircleShape)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Invite Member", tint = VLinkCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // In-Call Chat Overlay Toggle
                        IconButton(
                            onClick = { showInCallChat = !showInCallChat },
                            modifier = Modifier
                                .size(44.dp)
                                .background(if (showInCallChat) VLinkCyan else Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = "In-Call Chat", tint = if (showInCallChat) Color.Black else Color.White)
                        }
                    }

                    // In-Call Quick Chat Drawer Overlay
                    if (showInCallChat) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.9f)
                                .height(260.dp)
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638).copy(alpha = 0.95f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                Text("In-Call Messages", color = VLinkCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color.White.copy(alpha = 0.1f))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    inCallMessages.forEach { msg ->
                                        Text("• $msg", color = Color.White, fontSize = 12.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = inCallMessageInput,
                                        onValueChange = { inCallMessageInput = it },
                                        placeholder = { Text("Type message...", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    IconButton(
                                        onClick = {
                                            if (inCallMessageInput.isNotBlank()) {
                                                inCallMessages.add("You: $inCallMessageInput")
                                                inCallMessageInput = ""
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Send", tint = VLinkCyan)
                                    }
                                }
                            }
                        }
                    }

                    // Control Actions Dock (Mute, Camera, Audio, Hang up)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 36.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(36.dp))
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speakerphone Toggle
                            IconButton(
                                onClick = { isSpeakerOn = !isSpeakerOn },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        if (isSpeakerOn) VLinkCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Speaker",
                                    tint = if (isSpeakerOn) VLinkCyan else Color.White
                                )
                            }

                            // Camera Toggle
                            IconButton(
                                onClick = { isVideoOn = !isVideoOn },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        if (isVideoOn) VLinkCyan else Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Video",
                                    tint = if (isVideoOn) Color.Black else Color.White
                                )
                            }

                            // Mic Mute Toggle
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        if (isMuted) Color.White else Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute",
                                    tint = if (isMuted) Color.Black else Color.White
                                )
                            }

                            // Switch Camera (Front/Back)
                            if (isVideoOn) {
                                IconButton(
                                    onClick = { isFrontCamera = !isFrontCamera },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "Switch Camera",
                                        tint = Color.White
                                    )
                                }
                            }

                            // End Call Button
                            IconButton(
                                onClick = onEndCall,
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(CallEndRed, CircleShape)
                                    .testTag("end_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "End Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Dialog: Select Contact to Invite to Call
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Participant", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a contact to invite to the WebRTC call mesh:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(contacts) { contact ->
                            ListItem(
                                headlineContent = { Text(contact.displayName, fontWeight = FontWeight.Bold) },
                                leadingContent = { PulseAvatar(imageUrl = contact.profilePictureUrl, name = contact.displayName) },
                                trailingContent = {
                                    Button(
                                        onClick = {
                                            viewModel.inviteParticipantToGroupCall(contact.displayName, contact.profilePictureUrl)
                                            inCallMessages.add("Invited ${contact.displayName} to mesh...")
                                            showInviteDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Dial", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                },
                                modifier = Modifier.clickable { }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dynamic Bottom Sheet / Dialog: Interactive Participant Controls
    selectedParticipantForControls?.let { participant ->
        AlertDialog(
            onDismissRequest = { selectedParticipantForControls = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PulseAvatar(imageUrl = participant.avatarUrl, name = participant.name, size = 44.dp)
                    Column {
                        Text(participant.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(participant.stats.resolution, color = VLinkCyan, fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // Connection Status Description
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("WebRTC Stream Performance", color = VLinkCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Codec", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Text(participant.stats.codec, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bitrate", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Text("${participant.stats.bitrateKbps} kbps", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Packet Loss", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Text(String.format("%.1f%%", participant.stats.packetLossPct), color = if (participant.stats.packetLossPct > 5.0f) Color.Red else Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Latency", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Text("${participant.stats.latencyMs} ms", color = if (participant.stats.latencyMs > 150) Color.Yellow else Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Audio Playback Slider
                    Column {
                        Text("Local Audio Volume", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (participant.isMutedByMe) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (participant.isMutedByMe) Color.Red else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Slider(
                                value = if (participant.isMutedByMe) 0f else participant.volume,
                                onValueChange = { viewModel.adjustParticipantVolume(participant.id, it) },
                                valueRange = 0f..2.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = VLinkCyan,
                                    activeTrackColor = VLinkCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                            Text(text = "${(participant.volume * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Local Mute button
                        Button(
                            onClick = {
                                viewModel.muteParticipantLocally(participant.id, !participant.isMutedByMe)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (participant.isMutedByMe) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (participant.isMutedByMe) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = null,
                                tint = if (participant.isMutedByMe) Color.Red else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (participant.isMutedByMe) "Unmute" else "Mute Locally",
                                color = if (participant.isMutedByMe) Color.Red else Color.White,
                                fontSize = 11.sp
                            )
                        }

                        // Disconnect / Kick button
                        Button(
                            onClick = {
                                viewModel.disconnectParticipant(participant.id)
                                inCallMessages.add("Kicked ${participant.name} from call mesh.")
                                selectedParticipantForControls = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect", color = Color.Red, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedParticipantForControls = null }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = VLinkCyan)
                }
            }
        )
    }
}

@Composable
fun AnimatedAudioWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(7) { index ->
            val heightScale by infiniteTransition.animateFloat(
                initialValue = 10f,
                targetValue = 36f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400 + index * 100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barHeight_$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(heightScale.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VLinkCyan)
            )
        }
    }
}

