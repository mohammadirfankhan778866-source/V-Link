package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val context = androidx.compose.ui.platform.LocalContext.current

    var authMode by remember { mutableStateOf(0) } // 0 = Log In, 1 = Create Account
    var isSubmitting by remember { mutableStateOf(false) }
    var submitProgressText by remember { mutableStateOf("") }

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

    // Secure Forgot Password states
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetTab by remember { mutableStateOf(0) } // 0 = Email Reset Link, 1 = Direct In-App Reset
    var resetIdentifierInput by remember { mutableStateOf("") }
    var resetNewPasswordInput by remember { mutableStateOf("") }
    var resetConfirmPasswordInput by remember { mutableStateOf("") }
    var resetNewPasswordVisible by remember { mutableStateOf(false) }
    var resetConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isResetLoading by remember { mutableStateOf(false) }
    var resetStatusMessage by remember { mutableStateOf("") }
    var resetIsSuccess by remember { mutableStateOf(false) }

    // Google Account Setup Dialog states (Nickname, Username & Direct Login Password)
    var showGoogleSetupDialog by remember { mutableStateOf(false) }
    var googleEmailInput by remember { mutableStateOf("mohammadirfankhan778866@gmail.com") }
    var googleDisplayNameInput by remember { mutableStateOf("") }
    var googleUsernameInput by remember { mutableStateOf("") }
    var googlePasswordInput by remember { mutableStateOf("") }
    var googlePasswordVisible by remember { mutableStateOf(false) }
    var isGoogleSetupLoading by remember { mutableStateOf(false) }
    var googleSetupErrorMessage by remember { mutableStateOf("") }
    var isCheckingGoogleUsername by remember { mutableStateOf(false) }
    var isGoogleUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var googleUsernameStatusText by remember { mutableStateOf("") }
    var googlePhotoUrl by remember { mutableStateOf<String?>(null) }

    // Real-time checks: Username
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameStatusText by remember { mutableStateOf("") }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }

    // Real-time checks: Email
    var isCheckingEmail by remember { mutableStateOf(false) }
    var emailStatusText by remember { mutableStateOf("") }
    var isEmailAvailable by remember { mutableStateOf<Boolean?>(null) }

    var registrationErrorMessage by remember { mutableStateOf("") }

    // Real-time Email & Password calculations
    val isLoginEmail = loginBackHandleInput.contains("@") && !loginBackHandleInput.startsWith("@")
    val isLoginHandle = loginBackHandleInput.startsWith("@") || (!isLoginEmail && loginBackHandleInput.isNotBlank())
    val isLoginInputValid = if (isLoginEmail) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(loginBackHandleInput.trim()).matches()
    } else {
        loginBackHandleInput.trim().removePrefix("@").length >= 3
    }

    val isRegEmailValidFormat = emailInput.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput.trim()).matches()

    // Real-time password strength (0: None, 1: Weak, 2: Fair, 3: Strong)
    val passwordStrength = remember(registerPasswordInput) {
        if (registerPasswordInput.isEmpty()) 0
        else {
            var score = 1
            if (registerPasswordInput.length >= 8) score++
            if (registerPasswordInput.any { it.isDigit() } && registerPasswordInput.any { it.isLetter() }) score++
            if (registerPasswordInput.any { !it.isLetterOrDigit() } || registerPasswordInput.any { it.isUpperCase() }) score++
            score.coerceIn(1, 3)
        }
    }

    val passwordsMatch = registerPasswordInput.isNotEmpty() && confirmPasswordInput.isNotEmpty() && registerPasswordInput == confirmPasswordInput

    // Debounced username availability check
    LaunchedEffect(usernameInput) {
        val clean = usernameInput.lowercase().removePrefix("@").trim()
        if (clean.length < 3) {
            isUsernameAvailable = null
            usernameStatusText = if (clean.isEmpty()) "" else "Username must be at least 3 characters"
            return@LaunchedEffect
        }

        isCheckingUsername = true
        usernameStatusText = "Checking handle availability..."
        delay(350) // Debounce typing

        val available = viewModel.checkUsernameAvailable(clean)
        isCheckingUsername = false
        isUsernameAvailable = available
        usernameStatusText = if (available) "✅ @$clean is available!" else "❌ @$clean is already registered"
    }

    // Debounced Google account username availability check
    LaunchedEffect(googleUsernameInput) {
        val clean = googleUsernameInput.lowercase().removePrefix("@").trim()
        if (clean.length < 3) {
            isGoogleUsernameAvailable = null
            googleUsernameStatusText = if (clean.isEmpty()) "" else "Handle must be at least 3 characters"
            return@LaunchedEffect
        }

        isCheckingGoogleUsername = true
        googleUsernameStatusText = "Checking handle availability..."
        delay(350)

        val available = viewModel.checkUsernameAvailable(clean)
        isCheckingGoogleUsername = false
        isGoogleUsernameAvailable = available
        googleUsernameStatusText = if (available) "✅ @$clean is available!" else "❌ @$clean is already registered"
    }

    // Debounced registration email availability check
    LaunchedEffect(emailInput) {
        val clean = emailInput.trim().lowercase()
        if (clean.isEmpty()) {
            isEmailAvailable = null
            emailStatusText = ""
            return@LaunchedEffect
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(clean).matches()) {
            isEmailAvailable = false
            emailStatusText = "⚠️ Please enter a valid email (e.g. name@domain.com)"
            return@LaunchedEffect
        }

        isCheckingEmail = true
        emailStatusText = "Verifying email availability..."
        delay(350)

        val available = viewModel.checkEmailAvailable(clean)
        isCheckingEmail = false
        isEmailAvailable = available
        emailStatusText = if (available) "✅ Email is available for registration" else "❌ An account with this email already exists"
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
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Welcome back! Enter your handle (@username) or registered email address, and your password to sign in.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )

                            // Username or Email Field with real-time feedback
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = loginBackHandleInput,
                                    onValueChange = { 
                                        loginBackHandleInput = it 
                                        loginBackErrorMessage = ""
                                    },
                                    label = { Text("Username or Email") },
                                    placeholder = { Text("e.g. @irfan or email@domain.com") },
                                    leadingIcon = { 
                                        Icon(
                                            if (isLoginEmail) Icons.Default.Email else Icons.Default.Person, 
                                            contentDescription = null, 
                                            tint = VLinkCyan 
                                        ) 
                                    },
                                    trailingIcon = {
                                        if (loginBackHandleInput.isNotBlank()) {
                                            if (isLoginInputValid) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Valid format", tint = PulseGreen)
                                            } else {
                                                Icon(Icons.Default.Info, contentDescription = "Invalid format", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    enabled = !isSubmitting,
                                    isError = loginBackHandleInput.isNotBlank() && !isLoginInputValid,
                                    modifier = Modifier.fillMaxWidth().testTag("login_back_inline_input")
                                )

                                if (loginBackHandleInput.isNotBlank()) {
                                    val helperColor = if (isLoginInputValid) PulseGreen else MaterialTheme.colorScheme.error
                                    val helperText = when {
                                        isLoginEmail && isLoginInputValid -> "✓ Valid email format detected"
                                        isLoginEmail && !isLoginInputValid -> "⚠️ Incomplete email address (e.g. name@domain.com)"
                                        !isLoginEmail && isLoginInputValid -> "✓ Valid username handle format"
                                        else -> "⚠️ Username must be at least 3 characters"
                                    }
                                    Text(
                                        text = helperText,
                                        fontSize = 11.sp,
                                        color = helperColor,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            // Password Field with real-time feedback
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

                                if (loginPasswordInput.isNotBlank() && loginPasswordInput.length < 6) {
                                    Text(
                                        text = "⚠️ Password must be at least 6 characters",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        resetIdentifierInput = loginBackHandleInput.trim()
                                        resetNewPasswordInput = ""
                                        resetConfirmPasswordInput = ""
                                        resetStatusMessage = ""
                                        resetIsSuccess = false
                                        resetTab = 0
                                        showForgotPasswordDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.LockReset, contentDescription = null, tint = VLinkCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
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

                            if (isSubmitting) {
                                Surface(
                                    color = VLinkViolet.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = VLinkCyan
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = submitProgressText.ifEmpty { "Authenticating with Firebase..." },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
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
                                    submitProgressText = "Verifying authentication..."
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
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Signing In...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                } else {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Log In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "  OR  ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    isSubmitting = true
                                    submitProgressText = "Connecting with Google..."
                                    loginBackErrorMessage = ""
                                    coroutineScope.launch {
                                        val result = viewModel.performGoogleSignIn(context)
                                        isSubmitting = false
                                        if (result.first) {
                                            // Successfully signed in directly
                                        } else if (result.second.startsWith("SETUP_REQUIRED:")) {
                                            val parts = result.second.removePrefix("SETUP_REQUIRED:").split("###")
                                            val email = parts.getOrNull(0) ?: "mohammadirfankhan778866@gmail.com"
                                            val name = parts.getOrNull(1) ?: email.substringBefore("@")
                                            val photo = parts.getOrNull(2)
                                            googleEmailInput = email
                                            googleDisplayNameInput = name
                                            googleUsernameInput = name.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }.ifBlank { email.substringBefore("@") }
                                            googlePhotoUrl = photo
                                            googlePasswordInput = ""
                                            googleSetupErrorMessage = ""
                                            showGoogleSetupDialog = true
                                        } else {
                                            // Open Google Setup dialog with prefilled details
                                            val inputEmail = if (loginBackHandleInput.contains("@") && !loginBackHandleInput.startsWith("@")) {
                                                loginBackHandleInput.trim()
                                            } else "mohammadirfankhan778866@gmail.com"
                                            googleEmailInput = inputEmail
                                            googleDisplayNameInput = inputEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
                                            googleUsernameInput = inputEmail.substringBefore("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }
                                            googlePhotoUrl = null
                                            googlePasswordInput = ""
                                            googleSetupErrorMessage = ""
                                            showGoogleSetupDialog = true
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("google_sign_in_login_btn"),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sign in with Google", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // MODE 1: CREATE ACCOUNT
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. Username
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = VLinkCyan)
                                        } else if (isUsernameAvailable == true) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = PulseGreen)
                                        } else if (isUsernameAvailable == false) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Taken", tint = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    singleLine = true,
                                    enabled = !isSubmitting,
                                    isError = isUsernameAvailable == false,
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
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            // 2. Nick Name
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "2. Nick Name / Display Name",
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
                                    trailingIcon = {
                                        if (displayNameInput.isNotBlank()) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = PulseGreen)
                                        }
                                    },
                                    singleLine = true,
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth().testTag("reg_display_name_inline_input")
                                )
                            }

                            // 3. Email Address with Real-Time Validation
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "3. Email Address (Verified via Firebase)",
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
                                    placeholder = { Text("e.g. name@domain.com") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = VLinkCyan) },
                                    trailingIcon = {
                                        if (isCheckingEmail) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = VLinkCyan)
                                        } else if (isEmailAvailable == true) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Email Available", tint = PulseGreen)
                                        } else if (isEmailAvailable == false && emailInput.isNotBlank()) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Email Unavailable", tint = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    singleLine = true,
                                    enabled = !isSubmitting,
                                    isError = emailInput.isNotBlank() && isEmailAvailable == false,
                                    modifier = Modifier.fillMaxWidth().testTag("reg_email_inline_input")
                                )

                                if (emailStatusText.isNotEmpty()) {
                                    Text(
                                        text = emailStatusText,
                                        fontSize = 11.sp,
                                        color = when (isEmailAvailable) {
                                            true -> PulseGreen
                                            false -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            // 4. Password with Real-Time Strength Meter
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    placeholder = { Text("Enter a secure password") },
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

                                if (registerPasswordInput.isNotEmpty()) {
                                    // Password Strength Bar
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (passwordStrength) {
                                                    1 -> "Strength: Weak (add letters & numbers)"
                                                    2 -> "Strength: Fair (add symbols or uppercase)"
                                                    else -> "Strength: Strong 🔒"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = when (passwordStrength) {
                                                    1 -> MaterialTheme.colorScheme.error
                                                    2 -> Color(0xFFFF9800)
                                                    else -> PulseGreen
                                                }
                                            )
                                            Text(
                                                text = "${registerPasswordInput.length} chars",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            for (i in 1..3) {
                                                val barColor = when {
                                                    i <= passwordStrength && passwordStrength == 1 -> MaterialTheme.colorScheme.error
                                                    i <= passwordStrength && passwordStrength == 2 -> Color(0xFFFF9800)
                                                    i <= passwordStrength -> PulseGreen
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(barColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. Confirm Password with Real-Time Match Status
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    isError = confirmPasswordInput.isNotEmpty() && !passwordsMatch,
                                    modifier = Modifier.fillMaxWidth().testTag("reg_confirm_password_inline_input")
                                )

                                if (confirmPasswordInput.isNotEmpty()) {
                                    val matchText = if (passwordsMatch) "✓ Passwords match" else "⚠️ Passwords do not match yet"
                                    val matchColor = if (passwordsMatch) PulseGreen else MaterialTheme.colorScheme.error
                                    Text(
                                        text = matchText,
                                        fontSize = 11.sp,
                                        color = matchColor,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

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

                            if (isSubmitting) {
                                Surface(
                                    color = VLinkCyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = VLinkCyan
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = submitProgressText.ifEmpty { "Registering with Firebase Auth..." },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
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
                                    if (!isRegEmailValidFormat) {
                                        registrationErrorMessage = "Please enter a valid email format (e.g. name@domain.com)."
                                        return@Button
                                    }
                                    if (isEmailAvailable == false) {
                                        registrationErrorMessage = "This email is already registered. Please log in or use another email."
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
                                    submitProgressText = "Creating account in Firebase Auth..."
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
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Creating Account...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                } else {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "  OR  ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    isSubmitting = true
                                    submitProgressText = "Connecting with Google..."
                                    registrationErrorMessage = ""
                                    coroutineScope.launch {
                                        val result = viewModel.performGoogleSignIn(context)
                                        isSubmitting = false
                                        if (result.first) {
                                            // Signed in directly
                                        } else if (result.second.startsWith("SETUP_REQUIRED:")) {
                                            val parts = result.second.removePrefix("SETUP_REQUIRED:").split("###")
                                            val email = parts.getOrNull(0) ?: if (emailInput.isNotBlank()) emailInput else "mohammadirfankhan778866@gmail.com"
                                            val name = parts.getOrNull(1) ?: displayNameInput.ifBlank { email.substringBefore("@") }
                                            val photo = parts.getOrNull(2)
                                            googleEmailInput = email
                                            googleDisplayNameInput = name
                                            googleUsernameInput = if (usernameInput.isNotBlank()) usernameInput.removePrefix("@") else name.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }.ifBlank { email.substringBefore("@") }
                                            googlePhotoUrl = photo
                                            googlePasswordInput = registerPasswordInput
                                            googleSetupErrorMessage = ""
                                            showGoogleSetupDialog = true
                                        } else {
                                            val email = if (emailInput.isNotBlank()) emailInput.trim() else "mohammadirfankhan778866@gmail.com"
                                            val name = if (displayNameInput.isNotBlank()) displayNameInput.trim() else email.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
                                            val baseUsername = if (usernameInput.isNotBlank()) usernameInput.trim().removePrefix("@") else email.substringBefore("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }
                                            googleEmailInput = email
                                            googleDisplayNameInput = name
                                            googleUsernameInput = baseUsername
                                            googlePhotoUrl = null
                                            googlePasswordInput = registerPasswordInput
                                            googleSetupErrorMessage = ""
                                            showGoogleSetupDialog = true
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("google_sign_up_btn"),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Sign up with Google", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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

    // Google Account Setup Dialog (Nickname, Username & Direct Login Password)
    if (showGoogleSetupDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isGoogleSetupLoading) {
                    showGoogleSetupDialog = false
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Google Account Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Customize your nickname and handle, and create an account password so you can sign in directly anytime.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    // Google Email
                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Google Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = VLinkCyan) },
                        singleLine = true,
                        enabled = !isGoogleSetupLoading,
                        modifier = Modifier.fillMaxWidth().testTag("google_setup_email_input")
                    )

                    // Nickname / Display Name
                    OutlinedTextField(
                        value = googleDisplayNameInput,
                        onValueChange = { googleDisplayNameInput = it },
                        label = { Text("Nickname (Display Name)") },
                        placeholder = { Text("e.g. Irfan Khan") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = VLinkCyan) },
                        singleLine = true,
                        enabled = !isGoogleSetupLoading,
                        modifier = Modifier.fillMaxWidth().testTag("google_setup_nickname_input")
                    )

                    // Unique @Username
                    Column {
                        OutlinedTextField(
                            value = googleUsernameInput,
                            onValueChange = { googleUsernameInput = it.removePrefix("@").filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                            label = { Text("Unique Handle") },
                            placeholder = { Text("username") },
                            prefix = { Text("@", color = VLinkCyan, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = VLinkCyan) },
                            trailingIcon = {
                                if (isCheckingGoogleUsername) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VLinkCyan)
                                } else if (isGoogleUsernameAvailable != null) {
                                    Icon(
                                        imageVector = if (isGoogleUsernameAvailable == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isGoogleUsernameAvailable == true) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !isGoogleSetupLoading,
                            modifier = Modifier.fillMaxWidth().testTag("google_setup_username_input")
                        )
                        if (googleUsernameStatusText.isNotEmpty()) {
                            Text(
                                text = googleUsernameStatusText,
                                fontSize = 11.sp,
                                color = if (isGoogleUsernameAvailable == true) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    // Account Password (for direct email/username & password login)
                    OutlinedTextField(
                        value = googlePasswordInput,
                        onValueChange = { googlePasswordInput = it },
                        label = { Text("Account Password (min 6 chars)") },
                        placeholder = { Text("Create password for direct login") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VLinkCyan) },
                        trailingIcon = {
                            IconButton(onClick = { googlePasswordVisible = !googlePasswordVisible }) {
                                Icon(
                                    imageVector = if (googlePasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (googlePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isGoogleSetupLoading,
                        modifier = Modifier.fillMaxWidth().testTag("google_setup_password_input")
                    )

                    // Informational Box
                    Surface(
                        color = VLinkCyan.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = VLinkCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Setting this password enables you to log in anytime directly with your Google email or @handle on the Login screen!",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    if (googleSetupErrorMessage.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = googleSetupErrorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = googleEmailInput.trim()
                        val nickname = googleDisplayNameInput.trim()
                        val rawUsername = googleUsernameInput.removePrefix("@").trim()
                        val password = googlePasswordInput.trim()

                        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            googleSetupErrorMessage = "Please enter a valid Google email address."
                            return@Button
                        }
                        if (nickname.isBlank()) {
                            googleSetupErrorMessage = "Please enter your Nickname."
                            return@Button
                        }
                        if (rawUsername.length < 3) {
                            googleSetupErrorMessage = "Username must be at least 3 characters."
                            return@Button
                        }
                        if (password.length < 6) {
                            googleSetupErrorMessage = "Password must be at least 6 characters."
                            return@Button
                        }

                        isGoogleSetupLoading = true
                        googleSetupErrorMessage = ""
                        coroutineScope.launch {
                            val result = viewModel.registerOrLinkGoogleAccount(
                                emailInput = email,
                                displayNameInput = nickname,
                                usernameInput = rawUsername,
                                passwordInput = password,
                                photoUrlInput = googlePhotoUrl
                            )
                            isGoogleSetupLoading = false
                            if (result.first) {
                                showGoogleSetupDialog = false
                            } else {
                                googleSetupErrorMessage = result.second
                            }
                        }
                    },
                    enabled = !isGoogleSetupLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan, contentColor = Color.Black)
                ) {
                    if (isGoogleSetupLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Creating Account...")
                    } else {
                        Text("Save & Sign In", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGoogleSetupDialog = false },
                    enabled = !isGoogleSetupLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResetLoading) {
                    showForgotPasswordDialog = false
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = VLinkCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Password Reset", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Method Selector Tabs
                    TabRow(
                        selectedTabIndex = resetTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = VLinkCyan,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = resetTab == 0,
                            onClick = { 
                                resetTab = 0 
                                resetStatusMessage = ""
                            },
                            text = { Text("Email Reset Link", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = resetTab == 1,
                            onClick = { 
                                resetTab = 1 
                                resetStatusMessage = ""
                            },
                            text = { Text("Direct Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Account Identifier Input
                    OutlinedTextField(
                        value = resetIdentifierInput,
                        onValueChange = {
                            resetIdentifierInput = it
                            resetStatusMessage = ""
                        },
                        label = { Text("Registered Email or @username") },
                        placeholder = { Text("e.g. name@domain.com or @irfan") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = VLinkCyan) },
                        singleLine = true,
                        enabled = !isResetLoading,
                        modifier = Modifier.fillMaxWidth().testTag("reset_identifier_input")
                    )

                    if (resetTab == 0) {
                        Text(
                            text = "An official password reset link will be sent to the registered email address associated with this account. Check your inbox and spam folder to set your new password.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    } else {
                        // Direct Reset Mode
                        Text(
                            text = "Set a new password directly for your account. You can immediately log in after updating.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        OutlinedTextField(
                            value = resetNewPasswordInput,
                            onValueChange = {
                                resetNewPasswordInput = it
                                resetStatusMessage = ""
                            },
                            label = { Text("New Password (min 6 chars)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = VLinkCyan) },
                            trailingIcon = {
                                IconButton(onClick = { resetNewPasswordVisible = !resetNewPasswordVisible }) {
                                    Icon(
                                        imageVector = if (resetNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (resetNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isResetLoading,
                            modifier = Modifier.fillMaxWidth().testTag("reset_new_password_input")
                        )

                        OutlinedTextField(
                            value = resetConfirmPasswordInput,
                            onValueChange = {
                                resetConfirmPasswordInput = it
                                resetStatusMessage = ""
                            },
                            label = { Text("Confirm New Password") },
                            leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null, tint = VLinkCyan) },
                            trailingIcon = {
                                IconButton(onClick = { resetConfirmPasswordVisible = !resetConfirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (resetConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (resetConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            enabled = !isResetLoading,
                            modifier = Modifier.fillMaxWidth().testTag("reset_confirm_password_input")
                        )
                    }

                    // Status / Error / Success message
                    if (resetStatusMessage.isNotEmpty()) {
                        Surface(
                            color = if (resetIsSuccess) Color(0xFF1B5E20).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (resetIsSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (resetIsSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = resetStatusMessage,
                                    color = if (resetIsSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (resetIsSuccess) {
                    Button(
                        onClick = {
                            showForgotPasswordDialog = false
                            loginBackHandleInput = resetIdentifierInput.trim()
                            loginPasswordInput = resetNewPasswordInput.trim()
                            authMode = 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan, contentColor = Color.Black)
                    ) {
                        Text("Log In Now", fontWeight = FontWeight.Bold)
                    }
                } else if (resetTab == 0) {
                    Button(
                        onClick = {
                            if (resetIdentifierInput.isBlank()) {
                                resetStatusMessage = "Please enter your registered email address or @username."
                                resetIsSuccess = false
                                return@Button
                            }
                            isResetLoading = true
                            resetStatusMessage = ""
                            coroutineScope.launch {
                                val result = viewModel.requestPasswordResetLink(resetIdentifierInput.trim())
                                isResetLoading = false
                                resetStatusMessage = result.second
                                resetIsSuccess = result.first
                            }
                        },
                        enabled = !isResetLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan, contentColor = Color.Black)
                    ) {
                        if (isResetLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sending Email...")
                        } else {
                            Text("Send Reset Email", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (resetIdentifierInput.isBlank()) {
                                resetStatusMessage = "Please enter your registered email or @username."
                                resetIsSuccess = false
                                return@Button
                            }
                            if (resetNewPasswordInput.length < 6) {
                                resetStatusMessage = "New password must be at least 6 characters."
                                resetIsSuccess = false
                                return@Button
                            }
                            if (resetNewPasswordInput != resetConfirmPasswordInput) {
                                resetStatusMessage = "Passwords do not match. Please re-enter."
                                resetIsSuccess = false
                                return@Button
                            }
                            isResetLoading = true
                            resetStatusMessage = ""
                            coroutineScope.launch {
                                val result = viewModel.resetPasswordDirectly(
                                    emailOrUsernameInput = resetIdentifierInput.trim(),
                                    newPasswordInput = resetNewPasswordInput.trim()
                                )
                                isResetLoading = false
                                resetStatusMessage = result.second
                                resetIsSuccess = result.first
                                if (result.first) {
                                    loginBackHandleInput = resetIdentifierInput.trim()
                                    loginPasswordInput = resetNewPasswordInput.trim()
                                    authMode = 0
                                }
                            }
                        },
                        enabled = !isResetLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = VLinkCyan, contentColor = Color.Black)
                    ) {
                        if (isResetLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Updating...")
                        } else {
                            Text("Update Password", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isResetLoading
                ) {
                    Text(if (resetIsSuccess) "Close" else "Cancel")
                }
            }
        )
    }
}
