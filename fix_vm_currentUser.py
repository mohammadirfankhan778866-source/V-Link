import re

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "r") as f:
    vm = f.read()

vm = vm.replace("            _currentUser.value = updatedUser\n", "")

with open("app/src/main/java/com/example/ui/viewmodels/MainViewModel.kt", "w") as f:
    f.write(vm)
