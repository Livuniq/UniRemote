package com.example.universalremote.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.universalremote.ir.IrBrandCodes
import com.example.universalremote.ir.IrManagerHelper
import com.example.universalremote.model.ConnectionMode
import com.example.universalremote.model.RemoteCommand
import com.example.universalremote.model.RemoteUiState
import com.example.universalremote.model.TvBrand
import com.example.universalremote.wifi.SmartTvManager
import com.example.universalremote.wifi.DiscoveredTv
import com.example.universalremote.wifi.SmartTvScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val irHelper = IrManagerHelper(application)
    private val wifiManager = SmartTvManager(application)
    private val tvScanner = SmartTvScanner(application)
    private val prefs = application.getSharedPreferences("tv_remote_prefs", Context.MODE_PRIVATE)

    private val initialIp = prefs.getString("saved_tv_ip", "192.168.1.9") ?: "192.168.1.9"

    private val _uiState = MutableStateFlow(
        RemoteUiState(
            hasHardwareIr = irHelper.hasIrEmitter,
            tvIpAddress = initialIp,
            connectedTvName = prefs.getString("saved_tv_name", "Toshiba TV") ?: "Toshiba TV",
            isWifiConnected = true,
            isTvPaired = wifiManager.isDevicePaired(initialIp)
        )
    )
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    fun setConnectionMode(mode: ConnectionMode) {
        _uiState.update { it.copy(connectionMode = mode) }
        if (mode == ConnectionMode.WIFI_SMART_TV && !_uiState.value.isWifiConnected && _uiState.value.discoveredTvs.isEmpty()) {
            startAutoScan()
        }
    }

    fun startAutoScan() {
        if (_uiState.value.isScanning) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanProgress = 0f,
                    lastCommand = "Scanning Wi-Fi network for Smart TVs..."
                )
            }

            val foundTvs = tvScanner.autoScan(
                onTvFound = { tv ->
                    _uiState.update { current ->
                        val updatedList = (current.discoveredTvs.filter { it.ip != tv.ip } + tv)
                        current.copy(discoveredTvs = updatedList)
                    }
                },
                onProgress = { progress ->
                    _uiState.update { it.copy(scanProgress = progress) }
                }
            )

            _uiState.update { current ->
                val statusMsg = if (foundTvs.isEmpty()) {
                    "No Smart TVs found. Check Wi-Fi connection or enter IP manually."
                } else {
                    "Found ${foundTvs.size} Smart TV(s) on network!"
                }

                // If only one TV found or none selected yet, auto-select
                val autoSelect = foundTvs.firstOrNull()
                if (autoSelect != null && current.connectedTvName.isEmpty()) {
                    current.copy(
                        isScanning = false,
                        scanProgress = 1f,
                        lastCommand = statusMsg,
                        connectedTvName = autoSelect.name,
                        tvIpAddress = autoSelect.ip,
                        selectedBrand = autoSelect.brand,
                        isWifiConnected = true
                    )
                } else {
                    current.copy(
                        isScanning = false,
                        scanProgress = 1f,
                        lastCommand = statusMsg
                    )
                }
            }
        }
    }

    fun selectDiscoveredTv(tv: DiscoveredTv) {
        prefs.edit()
            .putString("saved_tv_ip", tv.ip)
            .putString("saved_tv_name", tv.name)
            .apply()

        val paired = wifiManager.isDevicePaired(tv.ip)
        _uiState.update {
            it.copy(
                connectedTvName = tv.name,
                tvIpAddress = tv.ip,
                selectedBrand = tv.brand,
                isWifiConnected = true,
                isTvPaired = paired,
                connectionMode = ConnectionMode.WIFI_SMART_TV,
                showWifiDialog = false,
                lastCommand = "Connected to ${tv.name} (${tv.ip})" + if (paired) " [Paired & Ready]" else " [Tap 'Pair TV' to link]"
            )
        }
    }

    fun startPairing() {
        val ip = _uiState.value.tvIpAddress
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPairingInProgress = true,
                    showPairingDialog = true,
                    pairingErrorMessage = null,
                    pairingSuccessMessage = null,
                    lastCommand = "Connecting to TV for pairing..."
                )
            }
            val result = wifiManager.startPairing(ip)
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        isPairingInProgress = false,
                        lastCommand = msg,
                        transmissionLog = listOf("✓ TV Pairing prompt displayed on TV screen!") + it.transmissionLog.take(9)
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isPairingInProgress = false,
                        pairingErrorMessage = err.message ?: "Failed to start pairing",
                        lastCommand = "Pairing error: ${err.message}"
                    )
                }
            }
        }
    }

    fun submitPairingCode(pin: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPairingInProgress = true,
                    pairingErrorMessage = null
                )
            }
            val result = wifiManager.submitPairingCode(pin)
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        isPairingInProgress = false,
                        showPairingDialog = false,
                        isTvPaired = true,
                        isWifiConnected = true,
                        lastCommand = msg,
                        transmissionLog = listOf("✓ $msg") + it.transmissionLog.take(9)
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isPairingInProgress = false,
                        pairingErrorMessage = err.message ?: "Incorrect code, please retry"
                    )
                }
            }
        }
    }

    fun dismissPairingDialog() {
        _uiState.update {
            it.copy(
                showPairingDialog = false,
                isPairingInProgress = false,
                pairingErrorMessage = null
            )
        }
    }

    fun setToshibaCodeSet(set: Int) {
        _uiState.update { it.copy(toshibaCodeSet = set, lastCommand = "Switched to Toshiba Code Set $set") }
    }

    fun selectBrand(brand: TvBrand) {
        _uiState.update { it.copy(selectedBrand = brand) }
    }

    fun updateTvIp(ip: String) {
        prefs.edit().putString("saved_tv_ip", ip).apply()
        _uiState.update { it.copy(tvIpAddress = ip) }
    }

    fun setWifiDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showWifiDialog = visible) }
    }

    fun setDiagnosticsDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showDiagnosticsDialog = visible) }
    }

    fun toggleKeypad() {
        _uiState.update { it.copy(showKeypad = !it.showKeypad) }
    }

    fun testWifiConnection() {
        val ip = _uiState.value.tvIpAddress
        viewModelScope.launch {
            _uiState.update { it.copy(lastCommand = "Testing Wi-Fi connection to $ip...") }
            val result = wifiManager.testConnection(ip)
            result.onSuccess { message ->
                _uiState.update {
                    it.copy(
                        isWifiConnected = true,
                        lastCommand = message,
                        transmissionLog = listOf("✓ $message") + it.transmissionLog.take(9)
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isWifiConnected = false,
                        lastCommand = "Failed: ${err.message}",
                        transmissionLog = listOf("✗ Wi-Fi test failed: ${err.message}") + it.transmissionLog.take(9)
                    )
                }
            }
        }
    }

    fun sendCommand(command: RemoteCommand) {
        performHapticFeedback()

        viewModelScope.launch {
            // Flash LED transmission effect
            _uiState.update { it.copy(isTransmitting = true, lastCommand = "Sending ${command.displayName}...") }

            val currentState = _uiState.value
            when (currentState.connectionMode) {
                ConnectionMode.IR_BLASTER -> {
                    val signal = IrBrandCodes.getSignal(currentState.selectedBrand, command, currentState.toshibaCodeSet)
                    val result = irHelper.transmit(signal.frequency, signal.pattern)
                    val logEntry = "[IR - ${currentState.selectedBrand.brandName}] ${command.displayName} (${signal.hexDisplay})"
                    _uiState.update {
                        it.copy(
                            lastCommand = "${command.displayName} sent (${signal.protocolName})",
                            transmissionLog = listOf(logEntry) + it.transmissionLog.take(9)
                        )
                    }
                }
                ConnectionMode.WIFI_SMART_TV -> {
                    val result = wifiManager.sendCommand(
                        tvIp = currentState.tvIpAddress,
                        brand = currentState.selectedBrand,
                        command = command
                    )

                    // Hybrid transmission: simultaneously send IR signal if phone has IR emitter
                    if (irHelper.hasIrEmitter) {
                        try {
                            val signal = IrBrandCodes.getSignal(currentState.selectedBrand, command, currentState.toshibaCodeSet)
                            irHelper.transmit(signal.frequency, signal.pattern)
                        } catch (_: Exception) {}
                    }

                    result.onSuccess { msg ->
                        _uiState.update {
                            it.copy(
                                isWifiConnected = true,
                                lastCommand = msg,
                                transmissionLog = listOf("[Wi-Fi + IR] ${command.displayName} -> $msg") + it.transmissionLog.take(9)
                            )
                        }
                    }.onFailure { err ->
                        val errMsg = err.message ?: "Command failed"
                        _uiState.update {
                            it.copy(
                                lastCommand = errMsg,
                                transmissionLog = listOf("[Wi-Fi Error] ${command.displayName}: $errMsg") + it.transmissionLog.take(9)
                            )
                        }
                        if (errMsg.contains("Pairing required", ignoreCase = true)) {
                            startPairing()
                        }
                    }
                }
            }

            delay(200)
            _uiState.update { it.copy(isTransmitting = false) }
        }
    }

    fun getIrDiagnostics(): String {
        return """
            Hardware IR Emitter: ${if (irHelper.hasIrEmitter) "YES (Supported)" else "NO (Simulated)"}
            Frequencies: ${irHelper.getCarrierFrequenciesDescription()}
            Selected Brand: ${_uiState.value.selectedBrand.brandName}
            Brand Default Freq: ${_uiState.value.selectedBrand.defaultFrequency / 1000} kHz
            Wi-Fi Target IP: ${_uiState.value.tvIpAddress}
        """.trimIndent()
    }

    private fun performHapticFeedback() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {
            // Ignore if vibration fails
        }
    }
}
