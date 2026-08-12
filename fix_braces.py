with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    lines = f.readlines()

# We need to remove line 152 and line 236 (assuming the line numbers are consistent with the latest `cat` output).
# Actually, the safest way is to remove lines that just have `    }` right after another `    }` and before the next `fun`.
new_lines = []
for i in range(len(lines)):
    if "    }" in lines[i] and i+1 < len(lines) and "    fun toggleFollowChannel" in lines[i+1] and "    }" in lines[i-1]:
        continue
    if "    }" in lines[i] and i+1 < len(lines) and "    fun toggleLikePost" in lines[i+1] and "    }" in lines[i-1]:
        continue
    
    new_lines.append(lines[i])

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.writelines(new_lines)
