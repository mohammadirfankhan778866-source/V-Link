package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PulseGreen

enum class EmojiCategory(val label: String, val icon: String) {
    SMILEYS("Smileys", "😀"),
    GESTURES("Gestures", "👍"),
    HEARTS("Hearts", "❤️"),
    ACTIVITIES("Fun & Life", "🎉"),
    NATURE("Animals & Nature", "🐶"),
    OBJECTS("Objects & Tech", "💡")
}

val EMOJI_MAP = mapOf(
    EmojiCategory.SMILEYS to listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥹", "😊", "😇", "🙂", "😉", "😌",
        "😍", "🥰", "😘", "😗", "😚", "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫",
        "🤔", "🫡", "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "😮‍💨", "🤥", "😌",
        "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵",
        "🤯", "🤠", "🥳", "🥸", "😎", "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳",
        "🥺", "😦", "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣", "😞", "😓", "😩",
        "😫", "🥱", "😤", "😡", "😠", "🤬", "😈", "👿", "💀", "💩", "🤡", "👻", "👽", "🤖"
    ),
    EmojiCategory.GESTURES to listOf(
        "👍", "👎", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦵",
        "🦶", "👂", "🦻", "👃", "👀", "👁️", "👅", "👄", "🫦", "👶", "👧", "🧒", "👦", "👩",
        "🧑", "👨", "👩‍🦱", "🧑‍🦱", "👨‍🦱", "👩‍🦰", "🧑‍🦰", "👨‍🦰", "👱‍♀️", "👱", "👱‍♂️", "👋", "🤚",
        "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉",
        "👆", "🖕", "👇", "☝️", "🫵", "✊", "👊", "🤛", "🤜", "🫶", "❤️‍🔥", "❤️‍🩹", "💯", "🔥"
    ),
    EmojiCategory.HEARTS to listOf(
        "❤️", "🩷", "🧡", "💛", "💚", "💙", "🩵", "💜", "🤎", "🖤", "🩶", "🤍", "💔", "❤️‍🔥",
        "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "💌", "💋", "✨", "🌟",
        "⭐", "💫", "⚡", "💥", "🔥", "🌈", "☀️", "🌙", "☁️", "❄️", "🪄", "🔮", "🧿", "💍"
    ),
    EmojiCategory.ACTIVITIES to listOf(
        "🎉", "🎊", "🎈", "🎁", "🏆", "🥇", "🥈", "🥉", "⚽", "🏀", "🏈", "⚾", "🎾", "🏐",
        "🏉", "🎱", "🎮", "🕹️", "🎲", "🧩", "🎭", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁",
        "🎷", "🎺", "🎸", "🪕", "🎻", "🚗", "🚀", "✈️", "⛵", "🚲", "🏖️", "⛺", "🏕️", "🎪"
    ),
    EmojiCategory.NATURE to listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷",
        "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄",
        "🐝", "🪱", "🐛", "🦋", "🐌", "🐞", "🐜", "🪰", "🪲", "🪳", "🪴", "🌲", "🌳", "🌴",
        "🌵", "🌷", "🌸", "🌹", "🌺", "🌻", "🌼", "🍀", "🍁", "🍂", "🍃", "🍄", "🥑", "🍓"
    ),
    EmojiCategory.OBJECTS to listOf(
        "💡", "🔦", "📱", "📲", "💻", "⌨️", "🖥️", "🖨️", "📷", "📸", "📹", "🎥", "📽️", "🎞️",
        "📞", "☎️", "📟", "📠", "📺", "📻", "🎙️", "🎚️", "🎛️", "🧭", "⏱️", "⏲️", "⏰", "🕰️",
        "⌛", "⏳", "📡", "🔋", "🪫", "🔌", "💸", "💵", "💴", "💶", "💷", "🪙", "💰", "💳",
        "💎", "⚖️", "🪜", "🧰", "🪛", "🔧", "🔨", "⚒️", "🛠️", "⛏️", "🪚", "🗝️", "🔑", "🔒"
    )
)

/**
 * Lightweight, fast emoji picker popup integrated above the message input field.
 */
@Composable
fun EmojiPickerPopup(
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }
    val currentEmojis = remember(selectedCategory) { EMOJI_MAP[selectedCategory] ?: emptyList() }
    val quickRecents = remember { listOf("❤️", "😂", "👍", "🔥", "🙏", "😍", "🎉", "✨", "🙌", "😊") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .testTag("emoji_picker_popup"),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Quick Recents / Frequent Row & Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    quickRecents.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onEmojiSelected(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 16.sp)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackspace,
                        modifier = Modifier.size(32.dp).testTag("emoji_backspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp).testTag("emoji_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Picker",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Emoji Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 40.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(currentEmojis, key = { it }) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEmojiSelected(emoji) }
                            .testTag("emoji_item_$emoji"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 22.sp
                        )
                    }
                }
            }

            // Category Bar Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiCategory.values().forEach { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) PulseGreen.copy(alpha = 0.25f) else Color.Transparent
                            )
                            .clickable { selectedCategory = category }
                            .testTag("emoji_category_${category.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.icon,
                            fontSize = if (isSelected) 20.sp else 16.sp
                        )
                    }
                }
            }
        }
    }
}
