import re

with open("app/src/main/java/com/example/data/models/Models.kt", "r") as f:
    content = f.read()

user_entity_replacement = """    val emailVerified: Boolean = false,
    val authProvider: String = "email",
    val chatProfilePictureUrl: String = "",
    val postProfilePictureUrl: String = "",
    val channelProfilePictureUrl: String = "",
    val channelAlias: String = "",
    val statusPrivacyMode: String = "PUBLIC",
    val statusPrivacyList: String = ""
)"""
content = re.sub(r'    val emailVerified: Boolean = false,\n    val authProvider: String = "email"\n\)', user_entity_replacement, content)


channel_entity_replacement = """    val isFollowedByMe: Boolean = false,
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val visibility: String = "PUBLIC" // PUBLIC, FRIENDS_ONLY
)"""
content = re.sub(r'    val isFollowedByMe: Boolean = false,\n    val lastMessageText: String = "",\n    val lastMessageTimestamp: Long = System\.currentTimeMillis\(\)\n\)', channel_entity_replacement, content)

post_entity_replacement = """    val isLikedByMe: Boolean = false,
    val fileExtension: String = "",
    val fileSize: String = "",
    val visibility: String = "PUBLIC" // PUBLIC, FRIENDS_ONLY
)"""
content = re.sub(r'    val isLikedByMe: Boolean = false,\n    val fileExtension: String = "",\n    val fileSize: String = ""\n\)', post_entity_replacement, content)

with open("app/src/main/java/com/example/data/models/Models.kt", "w") as f:
    f.write(content)
