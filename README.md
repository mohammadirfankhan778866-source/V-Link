# Pulse Chat ⚡ - Ultra-Fast Modern Messaging Application

Pulse Chat is a production-grade, secure, modern messaging platform inspired by WhatsApp. Built with a high-concurrency **Erlang/OTP** backend and a high-performance **Android Jetpack Compose** client.

---

## Architecture Overview

```
+-------------------------------------------------------+
|                    Android Client                     |
|  - Jetpack Compose M3 (Light / Dark / AMOLED)         |
|  - Room Local SQLite Persistence & Offline Auto-Sync  |
|  - Coroutines, Flow, StateFlow, ViewModel             |
+-------------------------------------------------------+
                           |
            WebSocket E2EE Signal Protocol
                           |
+-------------------------------------------------------+
|                 Erlang/OTP Gateway                    |
|  - Cowboy HTTP/WebSocket Engine                       |
|  - GenServer Supervision Trees (1M+ Connections)     |
|  - OTP Relup Hot-Code Swap Capability                 |
+-------------------------------------------------------+
                           |
          +----------------+----------------+
          |                                 |
+-------------------+             +-------------------+
| PostgreSQL DB     |             | Redis Cache       |
| User, Message,    |             | Presence &        |
| Chat Schema       |             | Rate Limiting     |
+-------------------+             +-------------------+
```

---

## Features

### 🔐 Authentication
- **Google OAuth / Sign-In**: Token-based authentication with automatic JWT session restoration. No phone/SMS OTP required.
- **Device Management**: Concurrent device login support with JWT token validation.

### 💬 Messaging Features
- **One-to-One & Group Chats**: Group creation, participant management, and admin controls.
- **Rich Media Sharing**: High-resolution image preview, video attachments, audio waveform voice notes, and document files.
- **Delivery & Read Status**: Live status ticks (Pending 🕒, Sent 🗸, Delivered 🗸🗸, Read 🗸🗸).
- **Interactive Actions**: Reply to message, Forward message, Star message, Reaction picker (❤️, 😂, 👍, 😮, 😢, 🔥), Delete for me, and Delete for everyone.
- **Customization**: Pinned chats, Archived chats, Chat wallpaper switcher (Emerald, Dark, AMOLED).
- **Typing & Presence**: Real-time typing indicators and live online status.

### 📞 Calls
- **Voice & Video Call Engine**: Full-screen active call overlay with duration timer, mute mic, toggle camera, speaker control, and WebRTC signal status.

### 🎨 Design & Themes
- **Material 3 UI**: Full edge-to-edge support with custom adaptive launcher icon.
- **Triple Theme Support**: Light Mode, Dark Mode, and True AMOLED Black Mode.

### 🛡️ Erlang Admin Dashboard
- **Real-Time Telemetry**: Active WebSockets counter, Erlang node cluster load, Redis cache hit rates, spam blocker, and live OTP Relup hot-code upgrade triggers.

---

## Android Build & Execution

1. **Prerequisites**: Android Studio Ladybug or newer, JDK 17, Gradle 8.x.
2. **Compile Applet**:
   Run `compile_applet` tool or `./gradlew assembleDebug`.
3. **Database**:
   Local database powered by Room KSP.

---

## License

MIT License. Built with high craftsmanship and privacy-first principles.
