package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PulseGreen
import com.example.ui.theme.VLinkCyan
import com.example.ui.theme.VLinkIndigo
import com.example.ui.theme.VLinkViolet
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onGoogleSignIn: (email: String, displayName: String, avatarUrl: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var authMode by remember { mutableStateOf(0) } // 0 = Log In, 1 = Create Account
    var isSubmitting by remember { mutableStateOf(false) }

    // Log In states
    var loginBackHandleInput by remember { mutableStateOf("") }
    var loginPasswordInput by remember { mutableStateOf("") }
    var loginBackErrorMessage by remember { mutableStateOf("") }

    // Register states
    var usernameInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var registerPasswordInput by remember { mutableStateOf("") }

    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameStatusText by remember { mutableStateOf("") }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var registrationErrorMessage by remember { mutableStateOf("") }

    // Debounced username availability check
    LaunchedEffect(usernameInput) {
        val clean = usernameInput.lowercase().removePrefix("@").trim()
        if (clean.length < 3) {
            isUsernameAvailable = null
            usernameStatusText = if (clean.isEmpty()) "" else "Username must be at least 3 characters"
            return@LaunchedEffect
        }

        isCheckingUsername = true
        usernameStatusText = "Checking availability in Firestore..."
        delay(400) // Debounce typing

        val available = viewModel.checkUsernameAvailable(clean)
        isCheckingUsername = false
        isUsernameAvailable = available
        usernameStatusText = if (available) "✅ @$clean is available!" else "❌ @$clean is already taken!"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Logo & V-Link Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vlink_new_logo_1786364050122),
                        contentDescription = "V-Link Official Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "V-Link",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Hyper-Speed Decentralized Messaging",
                    fontSize = 14.sp,
                    color = VLinkCyan,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Highlights Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VLinkCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = VLinkCyan)
                        }
                        Column {
                            Text("Unique Handle Identity", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Global unique username registry via Firestore", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VLinkViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = VLinkViolet)
                        }
                        Column {
                            Text("Firestore Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Realtime message backup & multi-device sync", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VLinkIndigo.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = VLinkIndigo)
                        }
                        Column {
                            Text("Encrypted Calls & Voice", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Ultra-low latency audio/video calling gateway", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Split-View Mode Switcher Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Segmented Navigation Toggles: [ Log In ] | [ Create Account ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (authMode == 0) VLinkViolet else Color.Transparent)
                                .clickable(enabled = !isSubmitting) { authMode = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Login,
                                    contentDescription = null,
                                    tint = if (authMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Log In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (authMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (authMode == 1) VLinkCyan else Color.Transparent)
                                .clickable(enabled = !isSubmitting) { authMode = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = if (authMode == 1) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Create Account",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (authMode == 1) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (authMode == 0) {
                        // MODE 0: LOG IN BACK
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Welcome back! Enter your handle or email address to log back into your V-Link account.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = loginBackHandleInput,
                                onValueChange = { loginBackHandleInput = it },
                                label = { Text("Username or Email") },
                                placeholder = { Text("e.g. irfan_vlink or irfan@vlink.chat") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = VLinkCyan) },
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("login_back_inline_input")
                            )

                            OutlinedTextField(
                                value = loginPasswordInput,
                                onValueChange = { loginPasswordInput = it },
                                label = { Text("Password") },
                                placeholder = { Text("Enter your password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VLinkCyan) },
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("login_back_password_input")
                            )

                            if (loginBackErrorMessage.isNotEmpty()) {
                                Text(
                                    text = loginBackErrorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    if (loginBackHandleInput.isBlank() || loginPasswordInput.isBlank()) {
                                        loginBackErrorMessage = "Please enter your username/email and password."
                                        return@Button
                                    }
                                    isSubmitting = true
                                    loginBackErrorMessage = ""
                                    coroutineScope.launch {
                                        val success = viewModel.performLoginBack(loginBackHandleInput, loginPasswordInput)
                                        isSubmitting = false
                                        if (!success) {
                                            loginBackErrorMessage = "Invalid credentials. Please verify your info or switch to 'Create Account'."
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("log_in_back_submit_btn"),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VLinkViolet, contentColor = Color.White)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Log In Back", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    } else {
                        // MODE 1: CREATE ACCOUNT
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "1. Unique Handle (@username). Firestore verifies no two users match.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("1. Unique Handle (@username)") },
                                placeholder = { Text("e.g. irfan_vlink") },
                                leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = VLinkCyan, modifier = Modifier.padding(start = 4.dp)) },
                                trailingIcon = {
                                    if (isCheckingUsername) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else if (isUsernameAvailable == true) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = PulseGreen)
                                    } else if (isUsernameAvailable == false) {
                                        Icon(Icons.Default.Error, contentDescription = "Taken", tint = MaterialTheme.colorScheme.error)
                                    }
                                },
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("reg_username_inline_input")
                            )

                            if (usernameStatusText.isNotEmpty()) {
                                Text(
                                    text = usernameStatusText,
                                    fontSize = 11.sp,
                                    color = when (isUsernameAvailable) {
                                        true -> PulseGreen
                                        false -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "2. Nick Name (you can put anything).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = displayNameInput,
                                onValueChange = { displayNameInput = it },
                                label = { Text("2. Nick Name") },
                                placeholder = { Text("e.g. Mohammad Irfan Khan") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = VLinkCyan) },
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("reg_display_name_inline_input")
                            )

                            Text(
                                text = "3. Email Address.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("3. Email Address") },
                                placeholder = { Text("e.g. irfan@vlink.chat") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = VLinkCyan) },
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("reg_email_inline_input")
                            )

                            Text(
                                text = "4. Password.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = registerPasswordInput,
                                onValueChange = { registerPasswordInput = it },
                                label = { Text("4. Password") },
                                placeholder = { Text("Enter a password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VLinkCyan) },
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("reg_password_inline_input")
                            )

                            if (registrationErrorMessage.isNotEmpty()) {
                                Text(
                                    text = registrationErrorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    if (usernameInput.isBlank()) {
                                        registrationErrorMessage = "Please enter a unique username handle."
                                        return@Button
                                    }
                                    if (isUsernameAvailable == false) {
                                        registrationErrorMessage = "Username is already taken in Firestore. Please choose another."
                                        return@Button
                                    }
                                    if (displayNameInput.isBlank()) {
                                        registrationErrorMessage = "Please enter a Nick Name."
                                        return@Button
                                    }
                                    if (emailInput.isBlank() || registerPasswordInput.isBlank()) {
                                        registrationErrorMessage = "Please enter an email address and a password."
                                        return@Button
                                    }
                                    if (registerPasswordInput.length < 6) {
                                        registrationErrorMessage = "Password must be at least 6 characters long."
                                        return@Button
                                    }
                                    isSubmitting = true
                                    registrationErrorMessage = ""
                                    coroutineScope.launch {
                                        val success = viewModel.registerUserWithUniqueUsername(
                                            displayName = displayNameInput.trim(),
                                            username = usernameInput.trim(),
                                            email = emailInput.trim(),
                                            password = registerPasswordInput
                                        )
                                        isSubmitting = false
                                        if (!success) {
                                            registrationErrorMessage = "Failed to register account. Please check your network connection or try a different email."
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("create_account_submit_btn"),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan, contentColor = Color.Black)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                                } else {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create V-Link Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    Text(
                        text = "By continuing, you agree to V-Link's Terms of Service & Privacy Policy.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
