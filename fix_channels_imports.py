with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    "import androidx.compose.material.icons.filled.Search",
    "import androidx.compose.material.icons.filled.Search\nimport androidx.compose.material.icons.filled.AccountCircle\nimport androidx.compose.material.icons.filled.Lock"
)

with open("app/src/main/java/com/example/ui/screens/ChannelsScreen.kt", "w") as f:
    f.write(content)
