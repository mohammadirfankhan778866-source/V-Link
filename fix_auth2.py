import re

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    content = f.read()

# I need to find `// Try Firebase Auth` and replace it with the method signature
content = content.replace('// Try Firebase Auth', '''
    suspend fun performLoginBack(usernameOrEmail: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val input = usernameOrEmail.trim()
            if (input.isEmpty() || password.isEmpty()) return@withContext false
            val isEmail = input.contains("@") && !input.startsWith("@")
            
            // Try Firebase Auth
''')

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.write(content)
