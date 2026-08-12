import re

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    content = f.read()

# 1. Remove saveLocalCredential function
content = re.sub(r'private fun saveLocalCredential.*?// 1. Check local Sandbox Credential first', '// 1. Checking Sandbox is disabled', content, flags=re.DOTALL)

# 2. In performLoginBack, remove the sandbox check
content = re.sub(r'// 1\. Checking Sandbox is disabled.*?// 2\. Real Firebase authentication fallback', '// Try Firebase Auth', content, flags=re.DOTALL)

# 3. In registerUserWithUniqueUsername, remove the sandbox fallback
content = re.sub(r'// --- SANDBOX / OFFLINE REGISTER FALLBACK.*?return@withContext true\n\s*\} catch \(e: Exception\) \{.*?\}', 'return@withContext false', content, flags=re.DOTALL)

# Also remove references to `saveLocalCredential` in Firebase success block
content = re.sub(r'saveLocalCredential\(.*?\)', '', content)

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.write(content)
