package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VLinkCyan
import com.example.ui.theme.VLinkViolet
import com.example.ui.viewmodels.NavigationTab

@Composable
fun GlassmorphicFloatingNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    items: List<NavigationTabItemData>,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var navMenuExpanded by remember { mutableStateOf(false) }

    // Glass background color
    val glassBgColor = if (isDark) {
        Color(0xFF141A29).copy(alpha = 0.82f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.85f)
    }

    // Glass border brush
    val glassBorder = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                VLinkCyan.copy(alpha = 0.35f),
                VLinkViolet.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.1f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.9f),
                VLinkCyan.copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.5f)
            )
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("main_bottom_navigation"),
        contentAlignment = Alignment.Center
    ) {
        // Floating Glass Capsule Container
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(36.dp),
                    ambientColor = if (isDark) VLinkCyan.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.1f),
                    spotColor = if (isDark) VLinkViolet.copy(alpha = 0.35f) else VLinkCyan.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(36.dp))
                .background(glassBgColor)
                .border(
                    width = 1.5.dp,
                    brush = glassBorder,
                    shape = RoundedCornerShape(36.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentTab == item.tab

                    // Spring-loaded scale animation
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.18f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "iconScale"
                    )

                    // Animated Active Glass Bubble Indicator
                    val activeBubbleColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isDark) VLinkCyan.copy(alpha = 0.22f) else VLinkCyan.copy(alpha = 0.18f)
                        } else Color.Transparent,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                        label = "bubbleColor"
                    )

                    // Animated Border on Selected Tab
                    val activeBorderColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isDark) VLinkCyan.copy(alpha = 0.4f) else VLinkCyan.copy(alpha = 0.3f)
                        } else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "activeBorderColor"
                    )

                    // Animated Icon Tint
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isDark) VLinkCyan else VLinkViolet
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        animationSpec = tween(durationMillis = 250),
                        label = "iconTint"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(activeBubbleColor)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = activeBorderColor,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = VLinkCyan)
                            ) {
                                onTabSelected(item.tab)
                            }
                            .testTag("nav_tab_${item.tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = iconTint,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(iconScale)
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = iconTint
                            )
                        }
                    }
                }

                // 3-Dots Overflow Menu Button for Navigation Bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isSettingsSelected = currentTab == NavigationTab.SETTINGS
                    val settingsTint = if (isSettingsSelected) {
                        if (isDark) VLinkCyan else VLinkViolet
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSettingsSelected) (if (isDark) VLinkCyan.copy(alpha = 0.2f) else VLinkCyan.copy(alpha = 0.15f)) else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = VLinkCyan)
                            ) {
                                navMenuExpanded = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("nav_bar_more_options_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = settingsTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "More",
                                fontSize = 11.sp,
                                fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Normal,
                                color = settingsTint
                            )
                        }

                        DropdownMenu(
                            expanded = navMenuExpanded,
                            onDismissRequest = { navMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("App Settings", fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    navMenuExpanded = false
                                    onTabSelected(NavigationTab.SETTINGS)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Settings, contentDescription = null, tint = VLinkCyan)
                                },
                                modifier = Modifier.testTag("navbar_menu_settings_item")
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavigationTabItemData(
    val tab: NavigationTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
