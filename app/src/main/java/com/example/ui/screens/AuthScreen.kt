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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    var loginPasswordVisible by remember { mutableStateOf(false) }
    var loginBackErrorMessage by remember { mutableStateOf("") }

    // Register states
    var usernameInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var registerPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var registerPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Forgot Password states
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordInput by remember { mutableStateOf("") }
    var isSendingResetLink by remember { mutableStateOf(false) }
    var forgotPasswordMessage by remember { mutableStateOf("") }
    var forgotPasswordIsSuccess by remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
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
                        painter = painterResource(id = R.drawable.vlink_formal_logo_1786379089872),
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
                                text = "Welcome back! Enter your handle (@username) or email address, and your password to sign in.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = loginBackHandleInput,
                                onValueChange = { 
                                    loginBackHandleInput = it 
                                    loginBackErrorMessage = ""
                                },
                                label = { Text("Username or Email") },
                                placeholder = { Text("e.g. @irfan_vlink or email@domain.com") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = VLinkCyan) },
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("login_back_inline_input")
                            )

                            OutlinedTextField(
                                value = loginPasswordInput,
                                onValueChange = { 
                                    loginPasswordInput = it 
                                    loginBackErrorMessage = ""
                                },
                                label = { Text("Password") },
                                placeholder = { Text("Enter your account password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VLinkCyan) },
                                trailingIcon = {
                                    IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                        Icon(
                                            imageVector = if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (loginPasswordVisible) "Hide password" else "Show password",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("login_back_password_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        forgotPasswordInput = if (loginBackHandleInput.isNotBlank()) loginBackHandleInput else ""
                                        forgotPasswordMessage = ""
                                        forgotPasswordIsSuccess = false
                                        showForgotPasswordDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        color = VLinkCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (loginBackErrorMessage.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = loginBackErrorMessage,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (loginBackHandleInput.isBlank() || loginPasswordInput.isBlank()) {
                                        loginBackErrorMessage = "Please enter your username or email and your password."
                                        return@Button
                                    }
                                    isSubmitting = true
                                    loginBackErrorMessage = ""
                                    coroutineScope.launch {
                                        val result = viewModel.performLoginBack(loginBackHandleInput, loginPasswordInput)
                                        isSubmitting = false
                                        if (!result.first) {
                                            loginBackErrorMessage = result.second
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
                                    Text("Log In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                text = "1. Unique Username (@handle)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { 
                                    usernameInput = it 
                                    registrationErrorMessage = ""
                                },
                                label = { Text("Unique Handle (@username)") },
                                placeholder = { Text("e.g. irfan_vlink") },
                                leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = VLinkCyan, modifier = Modifier.padding(start = 12.dp)) },
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
                                text = "2. Nick Name / Full Name",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = displayNameInput,
                                onValueChange = { 
                                    displayNameInput = it 
                                    registrationErrorMessage = ""
                                },
                                label = { Text("Nick Name") },
                                placeholder = { Text("e.g. Mohammad Irfan Khan") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = VLinkCyan) },
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("reg_display_name_inline_input")
                            )

                            Text(
                                text = "3. Email Address",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { 
                                    emailInput = it 
                                    registrationErrorMessage = ""
                                },
                                label = { Text("Email Address") },
                                placeholder = { Text("e.g. mohammadirfankhan778866@gmail.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = VLinkCyan) },
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("reg_email_inline_input")
                            )

                            Text(
                                text = "4. Password (min 6 characters)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = registerPasswordInput,
                                onValueChange = { 
                                    registerPasswordInput = it 
                                    registrationErrorMessage = ""
                                },
                                label = { Text("Password") },
                                placeholder = { Text("Enter your password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VLinkCyan) },
                                trailingIcon = {
                                    IconButton(onClick = { registerPasswordVisible = !registerPasswordVisible }) {
                                        Icon(
                                            imageVector = if (registerPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (registerPasswordVisible) "Hide password" else "Show password",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                visualTransformation = if (registerPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("reg_password_inline_input")
                            )

                            Text(
                                text = "5. Confirm Password",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = confirmPasswordInput,
                                onValueChange = { 
                                    confirmPasswordInput = it 
                                    registrationErrorMessage = ""
                                },
                                label = { Text("Confirm Password") },
                                placeholder = { Text("Re-enter your password") },
                                leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = VLinkCyan) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                enabled = !isSubmitting,
                                modifier = Modifier.fillMaxWidth().testTag("reg_confirm_password_inline_input")
                            )

                            if (registrationErrorMessage.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = registrationErrorMessage,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (usernameInput.isBlank()) {
                                        registrationErrorMessage = "Please enter a unique username handle."
                                        return@Button
                                    }
                                    if (isUsernameAvailable == false) {
                                        registrationErrorMessage = "Username is already taken. Please choose another."
                                        return@Button
                                    }
                                    if (displayNameInput.isBlank()) {
                                        registrationErrorMessage = "Please enter your Nick Name."
                                        return@Button
                                    }
                                    if (emailInput.isBlank()) {
                                        registrationErrorMessage = "Please enter your email address."
                                        return@Button
                                    }
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput.trim()).matches()) {
                                        registrationErrorMessage = "Please enter a valid email format (e.g. name@domain.com)."
                                        return@Button
                                    }
                                    if (registerPasswordInput.isBlank()) {
                                        registrationErrorMessage = "Please enter a password."
                                        return@Button
                                    }
                                    if (registerPasswordInput.length < 6) {
                                        registrationErrorMessage = "Password must be at least 6 characters long."
                                        return@Button
                                    }
                                    if (registerPasswordInput != confirmPasswordInput) {
                                        registrationErrorMessage = "Passwords do not match. Please ensure both passwords match."
                                        return@Button
                                    }

                                    isSubmitting = true
                                    registrationErrorMessage = ""
                                    coroutineScope.launch {
                                        val result = viewModel.registerUserWithUniqueUsername(
                                            displayName = displayNameInput.trim(),
                                            username = usernameInput.trim(),
                                            email = emailInput.trim(),
                                            password = registerPasswordInput
                                        )
                                        isSubmitting = false
                                        if (!result.first) {
                                            registrationErrorMessage = result.second
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
                                    Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSendingResetLink) {
                    showForgotPasswordDialog = false
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = VLinkCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your registered email address or @username. We will send a secure password reset link directly to your email.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = forgotPasswordInput,
                        onValueChange = {
                            forgotPasswordInput = it
                            forgotPasswordMessage = ""
                        },
                        label = { Text("Email or @username") },
                        placeholder = { Text("e.g. name@domain.com or @irfan") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = VLinkCyan) },
                        singleLine = true,
                        enabled = !isSendingResetLink,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (forgotPasswordMessage.isNotEmpty()) {
                        Surface(
                            color = if (forgotPasswordIsSuccess) Color(0xFF1B5E20).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = forgotPasswordMessage,
                                color = if (forgotPasswordIsSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotPasswordInput.isBlank()) {
                            forgotPasswordMessage = "Please enter your email or username."
                            forgotPasswordIsSuccess = false
                            return@Button
                        }
                        isSendingResetLink = true
                        forgotPasswordMessage = ""
                        coroutineScope.launch {
                            val result = viewModel.sendPasswordResetLink(forgotPasswordInput.trim())
                            isSendingResetLink = false
                            forgotPasswordIsSuccess = result.first
                            forgotPasswordMessage = result.second
                        }
                    },
                    enabled = !isSendingResetLink,
                    colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan, contentColor = Color.Black)
                ) {
                    if (isSendingResetLink) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sending...")
                    } else {
                        Text("Send Reset Link", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isSendingResetLink
                ) {
                    Text("Close")
                }
            }
        )
    }
}
