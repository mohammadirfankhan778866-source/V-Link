package com.example.data.models

enum class ParticipantConnectionState {
    INVITED,
    RINGING,
    CONNECTING,
    CONNECTED,
    MUTED,
    DISCONNECTED,
    DECLINED
}

data class WebRtcStats(
    val bitrateKbps: Int = 850,
    val packetLossPct: Float = 0.1f,
    val codec: String = "Opus HD / VP8",
    val latencyMs: Int = 42,
    val resolution: String = "1080p @ 30fps"
)

data class CallParticipant(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val isHost: Boolean = false,
    val connectionState: ParticipantConnectionState = ParticipantConnectionState.INVITED,
    val isAudioEnabled: Boolean = true,
    val isVideoEnabled: Boolean = true,
    val isSpeaking: Boolean = false,
    val volume: Float = 1.0f, // adjustable local playback volume
    val audioLevel: Float = 0.0f, // current voice amplitude 0f-1f
    val isMutedByMe: Boolean = false, // local listener mute
    val stats: WebRtcStats = WebRtcStats()
)
