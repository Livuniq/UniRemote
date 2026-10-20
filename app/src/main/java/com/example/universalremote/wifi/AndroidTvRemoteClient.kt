package com.example.universalremote.wifi

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.universalremote.model.RemoteCommand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.SSLSocket

/**
 * Handles pairing and remote key commands for Google Android TV Remote v2 (Ports 6467 and 6466).
 */
class AndroidTvRemoteClient(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("AndroidTvPrefs", Context.MODE_PRIVATE)

    private val _isPaired = MutableStateFlow(false)
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var activeTvIp: String? = null

    // Sockets
    private var pairingSocket: SSLSocket? = null
    private var remoteSocket: SSLSocket? = null
    private var remoteListenJob: Job? = null
    private var remoteReadyDeferred: CompletableDeferred<Boolean>? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "AndroidTvRemoteClient"
        private const val PAIR_PORT = 6467
        private const val REMOTE_PORT = 6466
    }

    fun isDevicePaired(tvIp: String): Boolean {
        return prefs.getBoolean("paired_$tvIp", false)
    }

    /**
     * Step 1: Connect to TV on port 6467 and trigger pairing PIN to appear on screen.
     */
    suspend fun startPairing(tvIp: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            closePairing()
            activeTvIp = tvIp

            val sslContext = AndroidTvKeyManager.getOrCreateSslContext(context)
            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            socket.soTimeout = 60000
            socket.connect(InetSocketAddress(tvIp, PAIR_PORT), 5000)
            socket.startHandshake()
            pairingSocket = socket

            val out = socket.outputStream
            val inp = socket.inputStream

            // Message 1: PairingRequest
            val pairingReq = hexToBytes("080210c801521c0a0961747672656d6f7465120f556e6976657273616c52656d6f7465")
            sendFramed(out, pairingReq)

            // Read PairingRequestAck
            val ack1 = readFramed(inp)
            Log.i(TAG, "Received PairingRequestAck: ${bytesToHex(ack1)}")

            // Message 2: Options
            val optionsMsg = hexToBytes("080210c801a201080a04080310061801")
            sendFramed(out, optionsMsg)

            // Read Options Ack
            val ack2 = readFramed(inp)
            Log.i(TAG, "Received Options: ${bytesToHex(ack2)}")

            // Message 3: Configuration
            val configMsg = hexToBytes("080210c801f201080a04080310061001")
            sendFramed(out, configMsg)

            // Read ConfigurationAck -> TV now displays the PIN!
            val ack3 = readFramed(inp)
            Log.i(TAG, "Received ConfigurationAck: ${bytesToHex(ack3)}. PIN should now be on TV screen.")

            Result.success("Pairing code requested. Check TV screen.")
        } catch (e: Exception) {
            Log.e(TAG, "startPairing failed", e)
            closePairing()
            Result.failure(e)
        }
    }

    /**
     * Step 2: User provides 6-character PIN displayed on TV screen.
     */
    suspend fun submitPairingCode(pin: String): Result<String> = withContext(Dispatchers.IO) {
        val socket = pairingSocket ?: return@withContext Result.failure(Exception("Pairing session expired or not active"))
        val tvIp = activeTvIp ?: return@withContext Result.failure(Exception("No TV IP selected"))

        try {
            val cleanPin = pin.trim().uppercase()
            if (cleanPin.length != 6) {
                return@withContext Result.failure(Exception("PIN must be 6 characters"))
            }

            // Get certificates
            val (_, clientCert) = AndroidTvKeyManager.getOrCreateCertificate(context)
            val serverCert = socket.session.peerCertificates[0] as java.security.cert.X509Certificate

            val clientPub = clientCert.publicKey as RSAPublicKey
            val serverPub = serverCert.publicKey as RSAPublicKey

            val clientMod = getModulusBytes(clientPub.modulus)
            val clientExp = getExponentBytes(clientPub.publicExponent)
            val serverMod = getModulusBytes(serverPub.modulus)
            val serverExp = getExponentBytes(serverPub.publicExponent)

            val pinPayloadHex = cleanPin.substring(2)
            val pinBytes = hexToBytes(pinPayloadHex)

            val md = MessageDigest.getInstance("SHA-256")
            md.update(clientMod)
            md.update(clientExp)
            md.update(serverMod)
            md.update(serverExp)
            md.update(pinBytes)
            val hash = md.digest()

            val expectedPrefix = cleanPin.substring(0, 2).toInt(16).toByte()
            if (hash[0] != expectedPrefix) {
                return@withContext Result.failure(Exception("Invalid PIN entered. Check characters and try again."))
            }

            // Construct secret message: 080210c801c202220a20 + hash
            val secretHeader = hexToBytes("080210c801c202220a20")
            val secretMessage = secretHeader + hash
            val out = socket.outputStream
            val inp = socket.inputStream

            sendFramed(out, secretMessage)

            // Read SecretAck
            val secretAck = readFramed(inp)
            Log.i(TAG, "Received SecretAck: ${bytesToHex(secretAck)}")

            // Pairing successful!
            prefs.edit().putBoolean("paired_$tvIp", true).apply()
            _isPaired.value = true
            closePairing()

            // Connect remote service immediately
            connectRemote(tvIp)

            Result.success("Pairing successful! TV connected.")
        } catch (e: Exception) {
            Log.e(TAG, "submitPairingCode failed", e)
            closePairing()
            Result.failure(e)
        }
    }

    /**
     * Establishes persistent connection on port 6466 for remote commands.
     */
    suspend fun connectRemote(tvIp: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_isConnected.value && remoteSocket != null && remoteSocket?.isClosed == false && activeTvIp == tvIp) {
            return@withContext Result.success(true)
        }

        try {
            disconnectRemote()
            activeTvIp = tvIp

            val readyDeferred = CompletableDeferred<Boolean>()
            remoteReadyDeferred = readyDeferred

            val sslContext = AndroidTvKeyManager.getOrCreateSslContext(context)
            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            socket.soTimeout = 0 // Keep alive
            socket.connect(InetSocketAddress(tvIp, REMOTE_PORT), 4000)
            socket.startHandshake()
            remoteSocket = socket

            val out = socket.outputStream
            val inp = socket.inputStream

            // Launch listener loop for handshake and keep-alive pings
            remoteListenJob = scope.launch {
                try {
                    while (isActive) {
                        val msg = readFramed(inp)
                        handleRemoteMessage(out, msg)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Remote socket disconnected: ${e.message}")
                } finally {
                    _isConnected.value = false
                    remoteReadyDeferred?.complete(false)
                }
            }

            // Wait for handshake to complete
            val isReady = withTimeoutOrNull(5000) { readyDeferred.await() } ?: false
            if (isReady) {
                _isConnected.value = true
                Log.i(TAG, "Android TV Remote connection ready!")
                Result.success(true)
            } else {
                Log.w(TAG, "Handshake timed out waiting for remote_start")
                _isConnected.value = true
                Result.success(true) // Attempt sending anyway
            }
        } catch (e: Exception) {
            Log.e(TAG, "connectRemote to $tvIp failed", e)
            disconnectRemote()
            Result.failure(e)
        }
    }

    private fun handleRemoteMessage(out: OutputStream, msg: ByteArray) {
        if (msg.isEmpty()) return
        val hex = bytesToHex(msg)

        // 1. RemoteConfigure request from TV (Tag 1 -> starts with 0x0A)
        if (msg[0] == 0x0A.toByte()) {
            val configResp = hexToBytes("0a1c08e704121718012201312a0961747672656d6f74653205312e302e30")
            sendFramed(out, configResp)
            Log.i(TAG, "Sent RemoteConfigure response (features: 615)")
        }
        // 2. RemoteSetActive request from TV (Tag 2 -> starts with 0x12)
        else if (msg[0] == 0x12.toByte()) {
            val activeResp = hexToBytes("120308e704")
            sendFramed(out, activeResp)
            Log.i(TAG, "Sent RemoteSetActive response (features: 615)")
        }
        // 3. RemoteStart (Tag 40 -> 0xC2 0x02)
        else if (msg.size >= 2 && msg[0] == 0xC2.toByte() && msg[1] == 0x02.toByte()) {
            Log.i(TAG, "Received RemoteStart: ready to send commands!")
            remoteReadyDeferred?.complete(true)
        }
        // 4. Ping request from TV (Tag 8 -> starts with 0x42)
        else if (msg.size >= 4 && msg[0] == 0x42.toByte()) {
            // Echo ping with response tag 0x4A
            val pingResp = msg.clone()
            pingResp[0] = 0x4A.toByte()
            sendFramed(out, pingResp)
        }
        else {
            // Other status messages (volume level, ime, etc.) also confirm connection is active
            remoteReadyDeferred?.complete(true)
        }
    }

    /**
     * Sends a key command to the Android TV.
     */
    suspend fun sendKey(tvIp: String, command: RemoteCommand): Result<String> = withContext(Dispatchers.IO) {
        if (!isDevicePaired(tvIp)) {
            return@withContext Result.failure(Exception("TV is not paired yet. Tap 'Pair TV' to connect."))
        }

        if (!_isConnected.value || remoteSocket == null || remoteSocket?.isClosed == true) {
            val connResult = connectRemote(tvIp)
            if (connResult.isFailure) {
                return@withContext Result.failure(connResult.exceptionOrNull() ?: Exception("Cannot connect to TV"))
            }
        }

        val socket = remoteSocket ?: return@withContext Result.failure(Exception("No active remote connection"))

        try {
            val keyCode = mapCommandToAndroidKeyCode(command)
            val keyPayload = buildKeyInjectPayload(keyCode)
            synchronized(this@AndroidTvRemoteClient) {
                sendFramed(socket.outputStream, keyPayload)
            }
            Log.i(TAG, "Successfully injected key $keyCode (${command.displayName}) over Wi-Fi")
            Result.success("Sent ${command.displayName} via Wi-Fi")
        } catch (e: Exception) {
            Log.e(TAG, "sendKey failed, resetting connection", e)
            disconnectRemote()
            Result.failure(e)
        }
    }

    private fun mapCommandToAndroidKeyCode(command: RemoteCommand): Int {
        return when (command) {
            RemoteCommand.POWER -> 26          // KEYCODE_POWER
            RemoteCommand.VOL_UP -> 24         // KEYCODE_VOLUME_UP
            RemoteCommand.VOL_DOWN -> 25       // KEYCODE_VOLUME_DOWN
            RemoteCommand.MUTE -> 164          // KEYCODE_VOLUME_MUTE
            RemoteCommand.NAV_UP -> 19         // KEYCODE_DPAD_UP
            RemoteCommand.NAV_DOWN -> 20       // KEYCODE_DPAD_DOWN
            RemoteCommand.NAV_LEFT -> 21       // KEYCODE_DPAD_LEFT
            RemoteCommand.NAV_RIGHT -> 22      // KEYCODE_DPAD_RIGHT
            RemoteCommand.NAV_OK -> 23         // KEYCODE_DPAD_CENTER
            RemoteCommand.BACK -> 4            // KEYCODE_BACK
            RemoteCommand.HOME -> 3            // KEYCODE_HOME
            RemoteCommand.MENU -> 82           // KEYCODE_MENU
            RemoteCommand.AV, RemoteCommand.SOURCE -> 178 // KEYCODE_TV_INPUT
            RemoteCommand.CH_UP -> 166         // KEYCODE_CHANNEL_UP
            RemoteCommand.CH_DOWN -> 167       // KEYCODE_CHANNEL_DOWN
            RemoteCommand.NUM_0 -> 7           // KEYCODE_0
            RemoteCommand.NUM_1 -> 8           // KEYCODE_1
            RemoteCommand.NUM_2 -> 9           // KEYCODE_2
            RemoteCommand.NUM_3 -> 10          // KEYCODE_3
            RemoteCommand.NUM_4 -> 11          // KEYCODE_4
            RemoteCommand.NUM_5 -> 12          // KEYCODE_5
            RemoteCommand.NUM_6 -> 13          // KEYCODE_6
            RemoteCommand.NUM_7 -> 14          // KEYCODE_7
            RemoteCommand.NUM_8 -> 15          // KEYCODE_8
            RemoteCommand.NUM_9 -> 16          // KEYCODE_9
        }
    }

    private fun buildKeyInjectPayload(keyCode: Int): ByteArray {
        val keyVarint = encodeVarint(keyCode)
        val innerLen = 1 + keyVarint.size + 2 // tag 0x08 (1) + varint + tag 0x10, 0x03 (2)
        val baos = ByteArrayOutputStream()
        baos.write(0x52) // Tag 10, wire type 2
        baos.write(innerLen)
        baos.write(0x08) // Tag 1 (key_code)
        baos.write(keyVarint)
        baos.write(0x10) // Tag 2 (direction)
        baos.write(0x03) // RemoteDirection.SHORT = 3
        return baos.toByteArray()
    }

    private fun encodeVarint(value: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            baos.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        baos.write(v and 0x7F)
        return baos.toByteArray()
    }

    private fun closePairing() {
        try { pairingSocket?.close() } catch (_: Exception) {}
        pairingSocket = null
    }

    fun disconnectRemote() {
        remoteListenJob?.cancel()
        remoteListenJob = null
        try { remoteSocket?.close() } catch (_: Exception) {}
        remoteSocket = null
        _isConnected.value = false
    }

    private fun getModulusBytes(modulus: BigInteger): ByteArray {
        val bytes = modulus.toByteArray()
        return if (bytes.size > 256 && bytes[0].toInt() == 0) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
    }

    private fun getExponentBytes(exponent: BigInteger): ByteArray {
        return byteArrayOf(0x01, 0x00, 0x01)
    }

    private fun sendFramed(out: OutputStream, payload: ByteArray) {
        writeVarint(out, payload.size)
        out.write(payload)
        out.flush()
    }

    private fun readFramed(inp: InputStream): ByteArray {
        val length = readVarint(inp)
        val buf = ByteArray(length)
        var total = 0
        while (total < length) {
            val r = inp.read(buf, total, length - total)
            if (r == -1) throw EOFException("Premature EOF reading framed message")
            total += r
        }
        return buf
    }

    private fun writeVarint(out: OutputStream, value: Int) {
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            out.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        out.write(v and 0x7F)
    }

    private fun readVarint(inp: InputStream): Int {
        var value = 0
        var shift = 0
        while (shift < 32) {
            val b = inp.read()
            if (b == -1) throw EOFException("Socket closed")
            value = value or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) return value
            shift += 7
        }
        throw IOException("Varint too long")
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim().replace(" ", "")
        val len = clean.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
        }
        return data
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
