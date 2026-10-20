package com.example.universalremote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.universalremote.model.ConnectionMode
import com.example.universalremote.model.RemoteCommand
import com.example.universalremote.model.TvBrand
import com.example.universalremote.ui.components.DpadControl
import com.example.universalremote.ui.components.IrDiagnosticsDialog
import com.example.universalremote.ui.components.PairingDialog
import com.example.universalremote.ui.components.RockerSwitch
import com.example.universalremote.ui.components.TactileButton
import com.example.universalremote.ui.components.WifiSettingsDialog
import androidx.compose.material.icons.filled.VpnKey

@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var brandMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF101216)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Infrared Emitter LED node
            TopIrLedIndicator(isTransmitting = state.isTransmitting)

            Spacer(modifier = Modifier.height(6.dp))

            // App Header
            Text(
                text = "UniRemote",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "UNIVERSAL SMART & IR REMOTE",
                color = Color(0xFF78909C),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Connection Mode Switcher (IR vs Wi-Fi)
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(Color(0xFF1E222B), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ConnectionModeChip(
                    title = "📡 IR Blaster",
                    selected = state.connectionMode == ConnectionMode.IR_BLASTER,
                    onClick = { viewModel.setConnectionMode(ConnectionMode.IR_BLASTER) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                ConnectionModeChip(
                    title = "📶 Wi-Fi TV",
                    selected = state.connectionMode == ConnectionMode.WIFI_SMART_TV,
                    onClick = { viewModel.setConnectionMode(ConnectionMode.WIFI_SMART_TV) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Brand Selection Row & Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    OutlinedButton(
                        onClick = { brandMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Brand: ${state.selectedBrand.brandName} ▼",
                            color = Color(0xFF90CAF9),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    DropdownMenu(
                        expanded = brandMenuExpanded,
                        onDismissRequest = { brandMenuExpanded = false }
                    ) {
                        TvBrand.values().forEach { brand ->
                            DropdownMenuItem(
                                text = { Text(brand.brandName) },
                                onClick = {
                                    viewModel.selectBrand(brand)
                                    brandMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.connectionMode == ConnectionMode.WIFI_SMART_TV) {
                        OutlinedButton(
                            onClick = { viewModel.setWifiDialogVisible(true) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Wifi,
                                contentDescription = "Wi-Fi Settings",
                                tint = if (state.isWifiConnected) Color(0xFF81C784) else Color(0xFF90CAF9),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.connectedTvName.isNotEmpty()) state.connectedTvName else "Auto Scan",
                                color = if (state.isWifiConnected) Color(0xFF81C784) else Color(0xFF90CAF9),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))

                        if (!state.isTvPaired) {
                            OutlinedButton(
                                onClick = { viewModel.startPairing() },
                                shape = RoundedCornerShape(10.dp),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFFB74D)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VpnKey,
                                    contentDescription = "Pair TV",
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pair TV",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                color = Color(0xFF1B5E20),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "✓ Paired",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(onClick = { viewModel.setDiagnosticsDialogVisible(true) }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Diagnostics",
                            tint = Color.LightGray
                        )
                    }
                }
            }

            // Toshiba Code Set selector
            if (state.selectedBrand == TvBrand.TOSHIBA && state.connectionMode == ConnectionMode.IR_BLASTER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Code Profile: ", fontSize = 11.sp, color = Color.Gray)
                    FilterChip(
                        selected = state.toshibaCodeSet == 1,
                        onClick = { viewModel.setToshibaCodeSet(1) },
                        label = { Text("Set 1 (Smart / CT-8560)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1976D2),
                            selectedLabelColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = state.toshibaCodeSet == 2,
                        onClick = { viewModel.setToshibaCodeSet(2) },
                        label = { Text("Set 2 (Regza / CT-90325)", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1976D2),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Remote Casing
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .shadow(12.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF242730), Color(0xFF16181F), Color(0xFF0F1116))
                        )
                    )
                    .border(1.dp, Color(0xFF333844), RoundedCornerShape(32.dp))
                    .padding(vertical = 20.dp, horizontal = 18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Row 1: POWER, TV/AV, INPUT, MUTE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Power ON/OFF
                        TactileButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.POWER) },
                            icon = Icons.Filled.PowerSettingsNew,
                            text = "POWER",
                            size = 58.dp,
                            primaryColor = Color(0xFFD32F2F),
                            highlightColor = Color(0xFFFF5252),
                            glowColor = Color(0xFFFF1744)
                        )

                        // Dedicated TV / AV Button
                        TactileButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.AV) },
                            text = "TV/AV",
                            size = 50.dp,
                            primaryColor = Color(0xFF283593),
                            highlightColor = Color(0xFF3949AB),
                            glowColor = Color(0xFF5C6BC0)
                        )

                        // Dedicated INPUT / SOURCE Button
                        TactileButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.SOURCE) },
                            text = "INPUT",
                            size = 50.dp,
                            primaryColor = Color(0xFF1A237E),
                            highlightColor = Color(0xFF303F9F)
                        )

                        // MUTE
                        TactileButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.MUTE) },
                            icon = Icons.AutoMirrored.Filled.VolumeMute,
                            text = "MUTE",
                            size = 50.dp,
                            primaryColor = Color(0xFF37474F),
                            highlightColor = Color(0xFF455A64)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Row 2: Rocker Switches (VOL +/- on Left, Keypad & Settings in center, CH +/- on Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume Rocker (VOL + / VOL -)
                        RockerSwitch(
                            title = "VOL",
                            onUpClick = { viewModel.sendCommand(RemoteCommand.VOL_UP) },
                            onDownClick = { viewModel.sendCommand(RemoteCommand.VOL_DOWN) },
                            isArrowType = false
                        )

                        // Center Utility Buttons (123 Keypad Toggle & Menu)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            TactileButton(
                                onClick = { viewModel.toggleKeypad() },
                                icon = Icons.Filled.Dialpad,
                                text = "123",
                                size = 46.dp,
                                primaryColor = if (state.showKeypad) Color(0xFF1976D2) else Color(0xFF2C303A),
                                highlightColor = if (state.showKeypad) Color(0xFF42A5F5) else Color(0xFF3D4350)
                            )
                            TactileButton(
                                onClick = { viewModel.sendCommand(RemoteCommand.MENU) },
                                icon = Icons.Filled.Menu,
                                text = "MENU",
                                size = 46.dp,
                                primaryColor = Color(0xFF2C303A),
                                highlightColor = Color(0xFF3D4350)
                            )
                        }

                        // Channel Rocker (CH + / CH -)
                        RockerSwitch(
                            title = "CH",
                            onUpClick = { viewModel.sendCommand(RemoteCommand.CH_UP) },
                            onDownClick = { viewModel.sendCommand(RemoteCommand.CH_DOWN) },
                            isArrowType = true
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // Row 3: 5-way D-Pad Navigation Control
                    DpadControl(
                        onUp = { viewModel.sendCommand(RemoteCommand.NAV_UP) },
                        onDown = { viewModel.sendCommand(RemoteCommand.NAV_DOWN) },
                        onLeft = { viewModel.sendCommand(RemoteCommand.NAV_LEFT) },
                        onRight = { viewModel.sendCommand(RemoteCommand.NAV_RIGHT) },
                        onOk = { viewModel.sendCommand(RemoteCommand.NAV_OK) }
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Row 4: Navigation auxiliary buttons: BACK and HOME
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TactileButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.BACK) },
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            text = "BACK",
                            size = 54.dp,
                            primaryColor = Color(0xFF30353E),
                            highlightColor = Color(0xFF424955)
                        )

                        TactileButton(
                            onClick = { viewModel.sendCommand(RemoteCommand.HOME) },
                            icon = Icons.Filled.Home,
                            text = "HOME",
                            size = 54.dp,
                            primaryColor = Color(0xFF1565C0),
                            highlightColor = Color(0xFF1E88E5),
                            glowColor = Color(0xFF42A5F5)
                        )
                    }

                    // Expandable Numeric Keypad (0-9)
                    AnimatedVisibility(visible = state.showKeypad) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NumericKeypadGrid(
                                onNumberClick = { num ->
                                    val cmd = when (num) {
                                        0 -> RemoteCommand.NUM_0
                                        1 -> RemoteCommand.NUM_1
                                        2 -> RemoteCommand.NUM_2
                                        3 -> RemoteCommand.NUM_3
                                        4 -> RemoteCommand.NUM_4
                                        5 -> RemoteCommand.NUM_5
                                        6 -> RemoteCommand.NUM_6
                                        7 -> RemoteCommand.NUM_7
                                        8 -> RemoteCommand.NUM_8
                                        else -> RemoteCommand.NUM_9
                                    }
                                    viewModel.sendCommand(cmd)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Status Console
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(Color(0xFF171A21), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF2A2E39), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.connectionMode == ConnectionMode.IR_BLASTER) {
                                if (state.hasHardwareIr) "🟢 IR Hardware Active" else "🟡 IR Simulation (No Phone IR)"
                            } else {
                                if (state.isWifiConnected) "🟢 Wi-Fi Connected" else "⚪ Wi-Fi (${state.tvIpAddress})"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF90A4AE),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = state.selectedBrand.brandName,
                            fontSize = 11.sp,
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.lastCommand,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE0E0E0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Dialogs
        if (state.showWifiDialog) {
            WifiSettingsDialog(
                currentIp = state.tvIpAddress,
                onIpChanged = { viewModel.updateTvIp(it) },
                onTestConnection = { viewModel.testWifiConnection() },
                onStartScan = { viewModel.startAutoScan() },
                onSelectTv = { viewModel.selectDiscoveredTv(it) },
                onDismiss = { viewModel.setWifiDialogVisible(false) },
                isConnected = state.isWifiConnected,
                isScanning = state.isScanning,
                scanProgress = state.scanProgress,
                discoveredTvs = state.discoveredTvs,
                statusMessage = state.lastCommand
            )
        }

        if (state.showDiagnosticsDialog) {
            IrDiagnosticsDialog(
                diagnosticsText = viewModel.getIrDiagnostics(),
                transmissionLogs = state.transmissionLog,
                onDismiss = { viewModel.setDiagnosticsDialogVisible(false) }
            )
        }

        if (state.showPairingDialog) {
            PairingDialog(
                tvName = state.connectedTvName.ifEmpty { "Smart TV" },
                tvIp = state.tvIpAddress,
                isPairingInProgress = state.isPairingInProgress,
                errorMessage = state.pairingErrorMessage,
                onSubmitPin = { pin -> viewModel.submitPairingCode(pin) },
                onDismiss = { viewModel.dismissPairingDialog() }
            )
        }
    }
}

@Composable
private fun TopIrLedIndicator(isTransmitting: Boolean) {
    val ledColor by animateColorAsState(
        targetValue = if (isTransmitting) Color(0xFFFF1744) else Color(0xFF37474F),
        animationSpec = tween(150),
        label = "led_glow"
    )
    val glowColor by animateColorAsState(
        targetValue = if (isTransmitting) Color(0xFFFF5252).copy(alpha = 0.6f) else Color.Transparent,
        animationSpec = tween(150),
        label = "led_halo"
    )

    Box(
        modifier = Modifier
            .padding(top = 10.dp)
            .size(24.dp)
            .background(glowColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .shadow(if (isTransmitting) 8.dp else 1.dp, CircleShape)
                .background(ledColor, CircleShape)
        )
    }
}

@Composable
private fun ConnectionModeChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF1976D2) else Color.Transparent)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else Color(0xFF333842),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NumericKeypadGrid(
    onNumberClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.85f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            (1..3).forEach { n ->
                TactileButton(
                    onClick = { onNumberClick(n) },
                    text = "$n",
                    size = 46.dp,
                    primaryColor = Color(0xFF252932),
                    highlightColor = Color(0xFF353A47)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            (4..6).forEach { n ->
                TactileButton(
                    onClick = { onNumberClick(n) },
                    text = "$n",
                    size = 46.dp,
                    primaryColor = Color(0xFF252932),
                    highlightColor = Color(0xFF353A47)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            (7..9).forEach { n ->
                TactileButton(
                    onClick = { onNumberClick(n) },
                    text = "$n",
                    size = 46.dp,
                    primaryColor = Color(0xFF252932),
                    highlightColor = Color(0xFF353A47)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TactileButton(
                onClick = { onNumberClick(0) },
                text = "0",
                size = 46.dp,
                primaryColor = Color(0xFF252932),
                highlightColor = Color(0xFF353A47)
            )
        }
    }
}
