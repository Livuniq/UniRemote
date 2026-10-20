package com.example.universalremote.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.universalremote.model.TvBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SmartTvScanner(private val context: Context) {

    private val discoveredMap = ConcurrentHashMap<String, DiscoveredTv>()

    /**
     * Finds the local IPv4 address and subnet prefix (e.g. "192.168.1.")
     * Prioritizes active Wi-Fi interface (wlan) and WifiManager
     */
    fun getLocalSubnetPrefix(): String? {
        try {
            // 1. Try WifiManager first (most accurate for active Wi-Fi connection)
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                val b1 = ipInt and 0xff
                val b2 = (ipInt shr 8) and 0xff
                val b3 = (ipInt shr 16) and 0xff
                val prefix = "$b1.$b2.$b3."
                Log.d("SmartTvScanner", "Subnet prefix from WifiManager: $prefix")
                return prefix
            }

            // 2. Iterate NetworkInterfaces, explicitly preferring 'wlan' interfaces
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            val wlanFirst = interfaces.sortedByDescending { it.name.contains("wlan", ignoreCase = true) }

            for (networkInterface in wlanFirst) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                // Skip virtual tunnel and proxy interfaces
                val name = networkInterface.name.lowercase()
                if (name.startsWith("vgate") || name.startsWith("dummy") || name.startsWith("tun")) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            val lastDot = host.lastIndexOf('.')
                            if (lastDot > 0) {
                                val prefix = host.substring(0, lastDot + 1)
                                Log.d("SmartTvScanner", "Subnet prefix from interface ${networkInterface.name}: $prefix")
                                return prefix
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmartTvScanner", "Error getting subnet prefix", e)
        }
        return "192.168.1."
    }

    /**
     * Executes automatic scan combining SSDP Multicast Discovery & Concurrent Subnet Port Scan
     */
    suspend fun autoScan(
        onTvFound: (DiscoveredTv) -> Unit,
        onProgress: (Float) -> Unit
    ): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        discoveredMap.clear()

        // 1. Run quick SSDP discovery in parallel
        val ssdpJob = async {
            performSsdpScan { tv ->
                if (!discoveredMap.containsKey(tv.ip)) {
                    discoveredMap[tv.ip] = tv
                    onTvFound(tv)
                }
            }
        }

        // 2. Run fast concurrent subnet port probe
        val subnetPrefix = getLocalSubnetPrefix() ?: "192.168.1."
        sendWakeOnLanBroadcast(subnetPrefix)
        val totalHosts = 254
        val completedCount = AtomicInteger(0)
        val concurrencyLimiter = Semaphore(40) // 40 concurrent sockets for speed & stability

        coroutineScope {
            val probeTasks = (1..totalHosts).map { hostIndex ->
                async {
                    concurrencyLimiter.withPermit {
                        val ip = "$subnetPrefix$hostIndex"
                        val tv = probeHostForSmartTv(ip)
                        if (tv != null && !discoveredMap.containsKey(tv.ip)) {
                            discoveredMap[tv.ip] = tv
                            onTvFound(tv)
                        }
                        val done = completedCount.incrementAndGet()
                        onProgress(done.toFloat() / totalHosts.toFloat())
                    }
                }
            }
            probeTasks.awaitAll()
        }

        try {
            ssdpJob.await()
        } catch (_: Exception) {}

        onProgress(1.0f)
        discoveredMap.values.toList()
    }

    private fun sendWakeOnLanBroadcast(subnetPrefix: String) {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            val payload = ByteArray(102) { 0xFF.toByte() }
            val p1 = DatagramPacket(payload, payload.size, InetAddress.getByName("255.255.255.255"), 9)
            socket.send(p1)
            val p2 = DatagramPacket(payload, payload.size, InetAddress.getByName("${subnetPrefix}255"), 9)
            socket.send(p2)
            socket.close()
        } catch (_: Exception) {}
    }

    /**
     * Probes an IP for known Smart TV ports and queries device info
     */
    private fun probeHostForSmartTv(ip: String): DiscoveredTv? {
        // Skip default gateway router IP
        if (ip.endsWith(".1")) return null

        // Priority 1: Check Google Cast / DIAL (8008) and Android TV Remote ports (6467, 6466, 8009, 8443)
        val androidTvPorts = listOf(8008, 6467, 6466, 8009, 8443)
        for (port in androidTvPorts) {
            if (isPortOpen(ip, port, timeoutMs = 280)) {
                val realName = fetchDialName(ip) ?: "Toshiba Android TV"
                val isToshiba = realName.contains("toshiba", ignoreCase = true) || realName.contains("hisense", ignoreCase = true)
                return DiscoveredTv(
                    name = realName,
                    ip = ip,
                    brand = if (isToshiba) TvBrand.TOSHIBA else TvBrand.GENERIC,
                    modelInfo = "Smart TV (Port $port)",
                    port = port
                )
            }
        }

        // Priority 2: Standard Smart TV ports
        val testPorts = listOf(8060, 8001, 8002, 3000, 8080, 5555)
        var openPort: Int? = null

        for (port in testPorts) {
            if (isPortOpen(ip, port, timeoutMs = 280)) {
                openPort = port
                break
            }
        }

        if (openPort != null) {
            return resolveTvDetails(ip, openPort)
        }

        // Priority 3: Port 80 - only if it is actually a Smart TV, NOT a WiFi IoT module
        if (isPortOpen(ip, 80, timeoutMs = 280)) {
            val isActualTv = verifyPort80IsTv(ip)
            if (isActualTv) {
                return DiscoveredTv(
                    name = "Smart TV ($ip)",
                    ip = ip,
                    brand = TvBrand.TOSHIBA,
                    modelInfo = "Smart TV HTTP",
                    port = 80
                )
            }
        }

        return null
    }

    private fun verifyPort80IsTv(ip: String): Boolean {
        return try {
            val url = URL("http://$ip/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 800
                readTimeout = 800
            }
            val html = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            conn.disconnect()
            // Ignore IoT modules, routers, switches, web consoles
            if (html.contains("WiFi Module", ignoreCase = true) ||
                html.contains("ESP8266", ignoreCase = true) ||
                html.contains("ESP32", ignoreCase = true) ||
                html.contains("Router", ignoreCase = true)) {
                false
            } else {
                html.contains("tv", ignoreCase = true) ||
                html.contains("toshiba", ignoreCase = true) ||
                html.contains("media", ignoreCase = true)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveTvDetails(ip: String, port: Int): DiscoveredTv {
        when (port) {
            8008, 8009 -> {
                val dialName = fetchDialName(ip) ?: "Smart TV (Google Cast / Android TV)"
                return DiscoveredTv(
                    name = dialName,
                    ip = ip,
                    brand = if (dialName.contains("toshiba", ignoreCase = true)) TvBrand.TOSHIBA else TvBrand.GENERIC,
                    modelInfo = "Google Cast / DIAL Port $port",
                    port = port
                )
            }
            8060 -> {
                // Roku TV ECP - fetch device info XML
                val name = fetchRokuName(ip) ?: "Roku Smart TV"
                return DiscoveredTv(
                    name = name,
                    ip = ip,
                    brand = TvBrand.ROKU,
                    modelInfo = "Roku ECP Port 8060",
                    port = 8060
                )
            }
            8001, 8002 -> {
                return DiscoveredTv(
                    name = "Samsung Smart TV",
                    ip = ip,
                    brand = TvBrand.SAMSUNG,
                    modelInfo = "Tizen Port $port",
                    port = port
                )
            }
            3000, 3001 -> {
                return DiscoveredTv(
                    name = "LG Smart TV",
                    ip = ip,
                    brand = TvBrand.LG,
                    modelInfo = "webOS Port $port",
                    port = port
                )
            }
            8080 -> {
                return DiscoveredTv(
                    name = "Toshiba / Smart TV",
                    ip = ip,
                    brand = TvBrand.TOSHIBA,
                    modelInfo = "Smart TV Port 8080",
                    port = 8080
                )
            }
            5555, 6466, 6467 -> {
                return DiscoveredTv(
                    name = "Android TV / Google TV",
                    ip = ip,
                    brand = TvBrand.GENERIC,
                    modelInfo = "Android TV Remote Port $port",
                    port = port
                )
            }
            80 -> {
                return DiscoveredTv(
                    name = "Connected Smart TV",
                    ip = ip,
                    brand = TvBrand.TOSHIBA,
                    modelInfo = "HTTP Port 80",
                    port = 80
                )
            }
            else -> {
                return DiscoveredTv(
                    name = "Smart TV ($ip)",
                    ip = ip,
                    brand = TvBrand.GENERIC,
                    modelInfo = "Port $port",
                    port = port
                )
            }
        }
    }

    private fun fetchDialName(ip: String): String? {
        return try {
            val url = URL("http://$ip:8008/ssdp/device-desc.xml")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 1200
                readTimeout = 1200
            }
            if (conn.responseCode == 200) {
                val xml = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                conn.disconnect()
                val fnStart = xml.indexOf("<friendlyName>")
                val fnEnd = xml.indexOf("</friendlyName>")
                if (fnStart != -1 && fnEnd != -1) {
                    xml.substring(fnStart + 14, fnEnd)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchRokuName(ip: String): String? {
        return try {
            val url = URL("http://$ip:8060/query/device-info")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 1200
                readTimeout = 1200
            }
            if (conn.responseCode == 200) {
                val content = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                conn.disconnect()
                val friendlyStart = content.indexOf("<friendly-device-name>")
                val friendlyEnd = content.indexOf("</friendly-device-name>")
                if (friendlyStart != -1 && friendlyEnd != -1) {
                    content.substring(friendlyStart + 22, friendlyEnd)
                } else {
                    val userStart = content.indexOf("<user-given-name>")
                    val userEnd = content.indexOf("</user-given-name>")
                    if (userStart != -1 && userEnd != -1) {
                        content.substring(userStart + 17, userEnd)
                    } else "Roku TV"
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * SSDP M-SEARCH broadcast over UDP
     */
    private fun performSsdpScan(onFound: (DiscoveredTv) -> Unit) {
        var multicastLock: WifiManager.MulticastLock? = null
        var socket: DatagramSocket? = null
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("SmartTvScannerLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            val ssdpQuery = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: ssdp:all\r\n\r\n"

            val queryBytes = ssdpQuery.toByteArray()
            val packet = DatagramPacket(
                queryBytes,
                queryBytes.size,
                InetAddress.getByName("239.255.255.250"),
                1900
            )

            socket = DatagramSocket().apply {
                soTimeout = 2200
                send(packet)
            }

            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 2200) {
                try {
                    val recvPacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(recvPacket)
                    val response = String(recvPacket.data, 0, recvPacket.length)
                    val ip = recvPacket.address.hostAddress ?: continue

                    // Check if response contains TV keywords
                    if (response.contains("roku", ignoreCase = true)) {
                        onFound(
                            DiscoveredTv(
                                name = "Roku TV",
                                ip = ip,
                                brand = TvBrand.ROKU,
                                modelInfo = "SSDP Discovery",
                                port = 8060
                            )
                        )
                    } else if (response.contains("samsung", ignoreCase = true)) {
                        onFound(
                            DiscoveredTv(
                                name = "Samsung Smart TV",
                                ip = ip,
                                brand = TvBrand.SAMSUNG,
                                modelInfo = "SSDP Discovery",
                                port = 8001
                            )
                        )
                    } else if (response.contains("webos", ignoreCase = true) || response.contains("lg", ignoreCase = true)) {
                        onFound(
                            DiscoveredTv(
                                name = "LG webOS TV",
                                ip = ip,
                                brand = TvBrand.LG,
                                modelInfo = "SSDP Discovery",
                                port = 3000
                            )
                        )
                    } else if (response.contains("dial", ignoreCase = true) || response.contains("GoogleCast", ignoreCase = true)) {
                        onFound(
                            DiscoveredTv(
                                name = "Smart TV (Google Cast / Android TV)",
                                ip = ip,
                                brand = TvBrand.GENERIC,
                                modelInfo = "DIAL Cast Protocol",
                                port = 8008
                            )
                        )
                    }
                } catch (_: Exception) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.w("SmartTvScanner", "SSDP scan error", e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (_: Exception) {}
        }
    }

    private fun isPortOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
