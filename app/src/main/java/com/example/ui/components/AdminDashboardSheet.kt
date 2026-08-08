package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AdminAnalytics
import com.example.ui.theme.PulseGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardSheet(
    onDismiss: () -> Unit
) {
    val analytics = remember { AdminAnalytics() }
    var hotSwapStatus by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = PulseGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Erlang/OTP Admin Console", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }

            // Cluster Status Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminMetricCard(
                    title = "Active WebSockets",
                    value = "89,410",
                    subtitle = "12 OTP Nodes",
                    icon = Icons.Default.CloudSync,
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = "Messages / Sec",
                    value = "14,280",
                    subtitle = "0.8ms Latency",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminMetricCard(
                    title = "Redis Cache",
                    value = "${analytics.redisCacheHitRate}%",
                    subtitle = "Hit Rate",
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = "Spam Blocked",
                    value = "${analytics.spamBlockedToday}",
                    subtitle = "Today",
                    icon = Icons.Default.Shield,
                    modifier = Modifier.weight(1f)
                )
            }

            // Server Load Meters
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("BEAM VM CLUSTER HEALTH", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PulseGreen)

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("BEAM CPU Load", fontSize = 13.sp)
                            Text("${analytics.serverCpuUsage}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { analytics.serverCpuUsage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = PulseGreen
                        )
                    }

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Memory Allocation", fontSize = 13.sp)
                            Text("${analytics.serverMemoryUsage}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { analytics.serverMemoryUsage / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = PulseGreen
                        )
                    }
                }
            }

            // OTP Hot Code Swap Trigger
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("HOT CODE RELUP MANAGEMENT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PulseGreen)
                    Text(
                        "Erlang/OTP hot code upgrades allow deploying backend beam module updates without dropping active WebSocket connections.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { hotSwapStatus = "Hot code upgrade release v2.4.0 deployed across 12 nodes in 0.04s! Zero dropped connections. 🚀" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hot_swap_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseGreen)
                    ) {
                        Text("Trigger OTP Relup Hot Swap", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (hotSwapStatus.isNotEmpty()) {
                        Text(
                            text = hotSwapStatus,
                            color = PulseGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = PulseGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
