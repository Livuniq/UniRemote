package com.example.universalremote.model

import com.example.universalremote.wifi.DiscoveredTv

/**
 * Standard remote control commands
 */
enum class RemoteCommand(val displayName: String) {
    POWER("Power ON/OFF"),
    AV("AV / Input"),
    VOL_UP("Volume +"),
    VOL_DOWN("Volume -"),
    MUTE("Mute"),
    CH_UP("Channel +"),
    CH_DOWN("Channel -"),
    NAV_UP("Up"),
    NAV_DOWN("Down"),
    NAV_LEFT("Left"),
    NAV_RIGHT("Right"),
    NAV_OK("OK / Enter"),
    BACK("Back"),
    HOME("Home"),
    MENU("Menu"),
    SOURCE("Input / Source"),
    NUM_0("0"),
    NUM_1("1"),
    NUM_2("2"),
    NUM_3("3"),
    NUM_4("4"),
    NUM_5("5"),
    NUM_6("6"),
    NUM_7("7"),
    NUM_8("8"),
    NUM_9("9")
}

/**
 * Connection modes supported by the universal remote
 */
enum class ConnectionMode(val title: String, val subtitle: String) {
    IR_BLASTER("Infrared (IR)", "For TVs with IR Sensor"),
    WIFI_SMART_TV("Wi-Fi Smart TV", "For Android TV, Roku, Tizen, webOS")
}

/**
 * Supported TV Brands
 */
enum class TvBrand(val brandName: String, val defaultFrequency: Int = 38000) {
    TOSHIBA("Toshiba", 38000),
    SAMSUNG("Samsung", 38000),
    LG("LG", 38000),
    SONY("Sony", 40000),
    MI_XIAOMI("Mi / Xiaomi", 38000),
    TCL("TCL / Thomson", 38000),
    PANASONIC("Panasonic", 36700),
    ROKU("Roku TV", 38000),
    GENERIC("Generic / Universal", 38000)
}

/**
 * UI State for the Remote Screen
 */
data class RemoteUiState(
    val connectionMode: ConnectionMode = ConnectionMode.IR_BLASTER,
    val selectedBrand: TvBrand = TvBrand.TOSHIBA,
    val toshibaCodeSet: Int = 1,
    val hasHardwareIr: Boolean = false,
    val tvIpAddress: String = "192.168.1.9",
    val connectedTvName: String = "Toshiba TV",
    val isWifiConnected: Boolean = true,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val discoveredTvs: List<DiscoveredTv> = emptyList(),
    val isTransmitting: Boolean = false,
    val lastCommand: String = "Ready",
    val transmissionLog: List<String> = emptyList(),
    val showWifiDialog: Boolean = false,
    val showDiagnosticsDialog: Boolean = false,
    val showKeypad: Boolean = false,
    val isTvPaired: Boolean = false,
    val showPairingDialog: Boolean = false,
    val isPairingInProgress: Boolean = false,
    val pairingErrorMessage: String? = null,
    val pairingSuccessMessage: String? = null
)
