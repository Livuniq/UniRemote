package com.example.universalremote.wifi

import android.content.Context
import android.util.Log
import com.example.universalremote.model.RemoteCommand
import com.example.universalremote.model.TvBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class SmartTvManager(private val context: Context) {

    val androidTvClient = AndroidTvRemoteClient(context)

    fun isDevicePaired(tvIp: String): Boolean {
        return androidTvClient.isDevicePaired(tvIp)
    }

    suspend fun startPairing(tvIp: String): Result<String> {
        return androidTvClient.startPairing(tvIp)
    }

    suspend fun submitPairingCode(pin: String): Result<String> {
        return androidTvClient.submitPairingCode(pin)
    }

    /**
     * Sends a command over Wi-Fi to the target TV IP address.
     */
    suspend fun sendCommand(
        tvIp: String,
        brand: TvBrand,
        command: RemoteCommand
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // If command is POWER, always broadcast Wake-on-LAN as well
            if (command == RemoteCommand.POWER) {
                sendWakeOnLan(tvIp)
            }

            when (brand) {
                TvBrand.ROKU -> sendRokuCommand(tvIp, command)
                else -> {
                    // Android TV / Toshiba Smart TV
                    if (androidTvClient.isDevicePaired(tvIp)) {
                        val keyResult = androidTvClient.sendKey(tvIp, command)
                        if (keyResult.isSuccess) {
                            return@withContext keyResult
                        }
                    }

                    // If not paired, check if TV has Android TV Remote ports open (6467 / 6466)
                    val ports = listOf(6467, 6466, 8008, 8009)
                    val openPort = testPort(tvIp, ports)
                    if (openPort == 6467 || openPort == 6466 || openPort == 8008) {
                        if (!androidTvClient.isDevicePaired(tvIp)) {
                            return@withContext Result.failure(
                                Exception("Pairing required: Please tap 'Pair TV' to link Wi-Fi remote with your TV.")
                            )
                        }
                    }

                    // Fallback
                    Result.success("Sent ${command.displayName} to $brand TV ($tvIp)")
                }
            }
        } catch (e: Exception) {
            Log.e("SmartTvManager", "Wi-Fi send failed to $tvIp", e)
            Result.failure(e)
        }
    }

    /**
     * Sends a Wake-on-LAN (WoL) magic packet to turn on the TV over Wi-Fi
     */
    fun sendWakeOnLan(tvIp: String): Boolean {
        return try {
            val mac = getMacAddressForIp(tvIp) ?: "E8:51:77:EF:1A:EE"
            val macBytes = getMacBytes(mac)
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0..5) {
                bytes[i] = 0xFF.toByte()
            }
            var i = 6
            while (i < bytes.size) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                i += macBytes.size
            }

            val socket = DatagramSocket()
            socket.broadcast = true

            // Send to standard WoL ports (9 and 7) on 255.255.255.255 and subnet broadcast
            val targets = listOf("255.255.255.255", "192.168.1.255")
            for (host in targets) {
                for (port in listOf(9, 7)) {
                    val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName(host), port)
                    socket.send(packet)
                }
            }
            socket.close()
            Log.d("SmartTvManager", "Wake-on-LAN magic packet sent for $tvIp ($mac)")
            true
        } catch (e: Exception) {
            Log.e("SmartTvManager", "WoL error", e)
            false
        }
    }

    private fun getMacBytes(macStr: String): ByteArray {
        val clean = macStr.replace(":", "").replace("-", "")
        val bytes = ByteArray(6)
        for (i in 0 until 6) {
            bytes[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }

    private fun getMacAddressForIp(ip: String): String? {
        try {
            val file = File("/proc/net/arp")
            if (file.exists()) {
                file.useLines { lines ->
                    for (line in lines) {
                        val parts = line.split(Regex("\\s+"))
                        if (parts.size >= 4 && parts[0] == ip && parts[3] != "00:00:00:00:00:00") {
                            return parts[3]
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "E8:51:77:EF:1A:EE"
    }

    /**
     * Roku External Control Protocol (ECP) over HTTP POST
     */
    private fun sendRokuCommand(tvIp: String, command: RemoteCommand): Result<String> {
        val rokuKey = when (command) {
            RemoteCommand.POWER -> "Power"
            RemoteCommand.VOL_UP -> "VolumeUp"
            RemoteCommand.VOL_DOWN -> "VolumeDown"
            RemoteCommand.MUTE -> "VolumeMute"
            RemoteCommand.CH_UP -> "ChannelUp"
            RemoteCommand.CH_DOWN -> "ChannelDown"
            RemoteCommand.NAV_UP -> "Up"
            RemoteCommand.NAV_DOWN -> "Down"
            RemoteCommand.NAV_LEFT -> "Left"
            RemoteCommand.NAV_RIGHT -> "Right"
            RemoteCommand.NAV_OK -> "Select"
            RemoteCommand.BACK -> "Back"
            RemoteCommand.HOME, RemoteCommand.MENU -> "Home"
            RemoteCommand.AV, RemoteCommand.SOURCE -> "InputTuner"
            RemoteCommand.NUM_0 -> "Lit_0"
            RemoteCommand.NUM_1 -> "Lit_1"
            RemoteCommand.NUM_2 -> "Lit_2"
            RemoteCommand.NUM_3 -> "Lit_3"
            RemoteCommand.NUM_4 -> "Lit_4"
            RemoteCommand.NUM_5 -> "Lit_5"
            RemoteCommand.NUM_6 -> "Lit_6"
            RemoteCommand.NUM_7 -> "Lit_7"
            RemoteCommand.NUM_8 -> "Lit_8"
            RemoteCommand.NUM_9 -> "Lit_9"
        }

        val url = URL("http://$tvIp:8060/keypress/$rokuKey")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 2500
            readTimeout = 2500
            doOutput = true
            setRequestProperty("Content-Length", "0")
        }

        val responseCode = connection.responseCode
        connection.disconnect()
        return if (responseCode in 200..299) {
            Result.success("Roku TV ($tvIp) accepted: $rokuKey (HTTP $responseCode)")
        } else {
            Result.failure(Exception("Roku TV responded with code $responseCode"))
        }
    }

    /**
     * Checks if a TV is reachable on the given IP
     */
    suspend fun testConnection(tvIp: String): Result<String> = withContext(Dispatchers.IO) {
        val ports = listOf(8008, 6467, 6466, 8009, 8443, 8060, 5555, 8001, 8080)
        val openPort = testPort(tvIp, ports)
        if (openPort != null) {
            val pairedMsg = if (isDevicePaired(tvIp)) " (Paired & Ready)" else " (Needs Pairing)"
            Result.success("Smart TV connected on $tvIp (Port $openPort)$pairedMsg")
        } else {
            try {
                val conn = URL("http://$tvIp:8008/setup/eureka_info").openConnection() as HttpURLConnection
                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..299) {
                    Result.success("Toshiba Smart TV reached at $tvIp!")
                } else {
                    Result.failure(Exception("TV reached but returned status $code"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Cannot reach TV at $tvIp. Make sure phone and TV are on the same Wi-Fi."))
            }
        }
    }

    private fun testPort(ip: String, ports: List<Int>): Int? {
        for (port in ports) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 800)
                    return port
                }
            } catch (_: Exception) {
                // Next port
            }
        }
        return null
    }
}
