import re

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    content = f.read()

# Replace the sandbox fallback block with just `return false`
content = re.sub(r'// --- SANDBOX / OFFLINE REGISTER FALLBACK.*?return@withContext false\n\s*\}', 'return@withContext false', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.write(content)
