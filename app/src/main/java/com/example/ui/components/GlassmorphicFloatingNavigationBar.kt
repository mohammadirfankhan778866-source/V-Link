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
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentTab == item.tab

                    // Animated Icon Scale
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "iconScale"
                    )

                    // Animated Active Glass Bubble Color
                    val activeBubbleColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isDark) VLinkCyan.copy(alpha = 0.2f) else VLinkCyan.copy(alpha = 0.15f)
                        } else Color.Transparent,
                        animationSpec = tween(durationMillis = 280),
                        label = "bubbleColor"
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
