package com.example.universalremote.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.universalremote.wifi.DiscoveredTv

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WifiSettingsDialog(
    currentIp: String,
    onIpChanged: (String) -> Unit,
    onTestConnection: () -> Unit,
    onStartScan: () -> Unit,
    onSelectTv: (DiscoveredTv) -> Unit,
    onDismiss: () -> Unit,
    isConnected: Boolean,
    isScanning: Boolean,
    scanProgress: Float,
    discoveredTvs: List<DiscoveredTv>,
    statusMessage: String
) {
    var ipText by remember { mutableStateOf(currentIp) }
    var showManualIp by remember { mutableStateOf(false) }

    // Auto-trigger scan if no TVs discovered yet
    LaunchedEffect(Unit) {
        if (discoveredTvs.isEmpty() && !isScanning) {
            onStartScan()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WifiFind,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto Scan Smart TV", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF64B5F6)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Connected Wi-Fi network par Smart TVs ko automatically search kiya ja raha hai.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scan Progress Bar
                if (isScanning) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Scanning network subnet...",
                                fontSize = 11.sp,
                                color = Color(0xFF90CAF9)
                            )
                            Text(
                                text = "${(scanProgress * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90CAF9)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF1976D2),
                            trackColor = Color(0xFF263238)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Discovered TVs Header & Refresh button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discovered TVs (${discoveredTvs.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    TextButton(
                        onClick = onStartScan,
                        enabled = !isScanning
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rescan", fontSize = 12.sp)
                    }
                }

                // Discovered TVs List
                if (discoveredTvs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFF1E222A), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isScanning) "Searching for Smart TVs..." else "No TVs discovered yet.\nTry Rescan or Manual IP below.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(discoveredTvs) { tv ->
                            val isThisConnected = isConnected && currentIp == tv.ip
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isThisConnected) Color(0xFF1B3A2B) else Color(0xFF21252E))
                                    .border(
                                        width = 1.dp,
                                        color = if (isThisConnected) Color(0xFF4CAF50) else Color(0xFF323846),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectTv(tv) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Tv,
                                        contentDescription = null,
                                        tint = if (isThisConnected) Color(0xFF81C784) else Color(0xFF64B5F6),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = tv.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${tv.ip} • ${tv.brand.brandName}",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (isThisConnected) {
                                    Text(
                                        text = "✓ Connected",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF81C784)
                                    )
                                } else {
                                    Button(
                                        onClick = { onSelectTv(tv) },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                                    ) {
                                        Text("Connect", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle for Manual IP
                TextButton(
                    onClick = { showManualIp = !showManualIp },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = if (showManualIp) "Hide Manual IP ▲" else "Enter Manual IP ▼",
                        fontSize = 11.sp,
                        color = Color(0xFF90CAF9)
                    )
                }

                AnimatedVisibility(visible = showManualIp) {
                    Column {
                        OutlinedTextField(
                            value = ipText,
                            onValueChange = {
                                ipText = it
                                onIpChanged(it)
                            },
                            label = { Text("TV IP Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("192.168.1.100", "192.168.1.50", "192.168.0.100").forEach { preset ->
                                OutlinedButton(
                                    onClick = {
                                        ipText = preset
                                        onIpChanged(preset)
                                    },
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(preset, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF20232A), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (isConnected) Color(0xFF81C784) else Color(0xFFFFB74D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onTestConnection,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("Test Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
