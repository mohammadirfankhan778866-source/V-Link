package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.ui.components.AdminDashboardSheet
import com.example.ui.components.CallActiveOverlay
import com.example.ui.components.GlassmorphicFloatingNavigationBar
import com.example.ui.components.NavigationTabItemData
import com.example.ui.viewmodels.MainViewModel
import com.example.ui.viewmodels.NavigationTab

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val activeChatId by viewModel.activeChatId.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isCallActiveScreenOpen by viewModel.isCallActiveScreenOpen.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val isAdminDashboardOpen by viewModel.isAdminDashboardOpen.collectAsState()

    var showNewChatModal by remember { mutableStateOf(false) }

    if (!isLoggedIn) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AuthScreen(
            viewModel = viewModel,
            onGoogleSignIn = { email, name, avatar ->
                viewModel.performGoogleLogin(context, email, name, avatar)
            }
        )
    } else if (activeChatId != null) {
        ChatDetailScreen(
            chatId = activeChatId!!,
            viewModel = viewModel,
            onBack = { viewModel.closeChatDetail() }
        )
    } else {
        Scaffold(
            bottomBar = {
                GlassmorphicFloatingNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    items = listOf(
                        NavigationTabItemData(
                            tab = NavigationTab.CHATS,
                            label = "Chats",
                            selectedIcon = Icons.Default.Chat,
                            unselectedIcon = Icons.Outlined.Chat
                        ),
                        NavigationTabItemData(
                            tab = NavigationTab.UPDATES,
                            label = "Updates",
                            selectedIcon = Icons.Default.DonutLarge,
                            unselectedIcon = Icons.Outlined.DonutLarge
                        ),
                        NavigationTabItemData(
                            tab = NavigationTab.POSTS,
                            label = "Posts",
                            selectedIcon = Icons.Default.RssFeed,
                            unselectedIcon = Icons.Outlined.RssFeed
                        ),
                        NavigationTabItemData(
                            tab = NavigationTab.CALLS,
                            label = "Calls",
                            selectedIcon = Icons.Default.Call,
                            unselectedIcon = Icons.Outlined.Call
                        ),
                        NavigationTabItemData(
                            tab = NavigationTab.SETTINGS,
                            label = "Settings",
                            selectedIcon = Icons.Default.Settings,
                            unselectedIcon = Icons.Outlined.Settings
                        )
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    NavigationTab.CHATS -> ChatsScreen(
                        viewModel = viewModel,
                        onOpenNewChatModal = { showNewChatModal = true }
                    )
                    NavigationTab.UPDATES -> StatusScreen(viewModel = viewModel)
                    NavigationTab.POSTS -> PostsScreen(viewModel = viewModel)
                    NavigationTab.CALLS -> CallsScreen(viewModel = viewModel)
                    NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // New Chat / New Group Modal
    if (showNewChatModal) {
        NewChatModal(
            viewModel = viewModel,
            onDismiss = { showNewChatModal = false }
        )
    }

    // Active Voice/Video Call Overlay
    if (isCallActiveScreenOpen && activeCall != null) {
        CallActiveOverlay(
            call = activeCall!!,
            viewModel = viewModel,
            onEndCall = { viewModel.endCall() }
        )
    }

    // Admin Dashboard Sheet
    if (isAdminDashboardOpen) {
        AdminDashboardSheet(
            onDismiss = { viewModel.toggleAdminDashboard(false) }
        )
    }
}

@Composable
fun RowScope.NavigationTabItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label
            )
        },
        label = { Text(label) }
    )
}
