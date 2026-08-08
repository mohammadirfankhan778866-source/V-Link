-- Pulse Chat Database Schema (PostgreSQL)

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    username VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    profile_picture_url TEXT,
    bio TEXT,
    online_status VARCHAR(32) DEFAULT 'OFFLINE',
    last_seen_timestamp BIGINT,
    account_created_date DATE DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS chats (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    is_group BOOLEAN DEFAULT FALSE,
    avatar_url TEXT,
    last_message_text TEXT,
    last_message_timestamp BIGINT,
    unread_count INT DEFAULT 0,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    wallpaper_theme VARCHAR(64) DEFAULT 'DEFAULT'
);

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(64) PRIMARY KEY,
    chat_id VARCHAR(64) REFERENCES chats(id) ON DELETE CASCADE,
    sender_id VARCHAR(64) REFERENCES users(id),
    sender_name VARCHAR(128),
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    status VARCHAR(32) DEFAULT 'SENT',
    type VARCHAR(32) DEFAULT 'TEXT',
    media_url TEXT,
    file_name VARCHAR(255),
    file_size VARCHAR(64),
    reply_to_message_id VARCHAR(64),
    is_starred BOOLEAN DEFAULT FALSE,
    is_deleted_for_everyone BOOLEAN DEFAULT FALSE,
    reactions TEXT
);

CREATE INDEX idx_messages_chat_id ON messages(chat_id);
CREATE INDEX idx_messages_timestamp ON messages(timestamp DESC);
CREATE INDEX idx_users_email ON users(email);
