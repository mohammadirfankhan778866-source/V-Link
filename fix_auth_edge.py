import re

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    '    Surface(\n        modifier = Modifier.fillMaxSize(),',
    '    Surface(\n        modifier = Modifier.fillMaxSize().systemBarsPadding(),'
)

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(content)
