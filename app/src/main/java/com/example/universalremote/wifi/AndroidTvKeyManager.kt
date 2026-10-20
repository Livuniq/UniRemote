package com.example.universalremote.wifi

import android.content.Context
import android.util.Log
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import java.net.Socket
import java.security.Principal

/**
 * Manages RSA keypair and self-signed X.509 certificate for Google Android TV Remote v2 protocol.
 */
object AndroidTvKeyManager {
    private const val TAG = "AndroidTvKeyManager"
    private const val CERT_FILE = "atv_remote_cert.der"
    private const val KEY_FILE = "atv_remote_key.pk8"
    private const val ALIAS = "atvremote"

    private var cachedKeyPair: KeyPair? = null
    private var cachedCertificate: X509Certificate? = null
    private var cachedSslContext: SSLContext? = null

    @Synchronized
    fun getOrCreateSslContext(context: Context): SSLContext {
        cachedSslContext?.let { return it }

        val (keyPair, certificate) = getOrCreateCertificate(context)

        // Custom KeyManager that unconditionally provides this client certificate
        val keyManager = object : X509ExtendedKeyManager() {
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> = arrayOf(ALIAS)
            override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String = ALIAS
            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null
            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? = null
            override fun getCertificateChain(alias: String?): Array<X509Certificate>? = arrayOf(certificate)
            override fun getPrivateKey(alias: String?): PrivateKey? = keyPair.private
        }

        // Trust-all TrustManager for TV's self-signed TLS cert
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(arrayOf<KeyManager>(keyManager), arrayOf<TrustManager>(trustManager), SecureRandom())
        }

        cachedSslContext = sslContext
        return sslContext
    }

    @Synchronized
    fun getOrCreateCertificate(context: Context): Pair<KeyPair, X509Certificate> {
        if (cachedKeyPair != null && cachedCertificate != null) {
            return Pair(cachedKeyPair!!, cachedCertificate!!)
        }

        val certFile = File(context.filesDir, CERT_FILE)
        val keyFile = File(context.filesDir, KEY_FILE)

        if (certFile.exists() && keyFile.exists()) {
            try {
                val cf = CertificateFactory.getInstance("X.509")
                val cert = FileInputStream(certFile).use { cf.generateCertificate(it) as X509Certificate }
                val keyBytes = keyFile.readBytes()
                val kf = KeyFactory.getInstance("RSA")
                val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(keyBytes))
                val keyPair = KeyPair(cert.publicKey, privKey)

                cachedKeyPair = keyPair
                cachedCertificate = cert
                return Pair(keyPair, cert)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load saved key/cert, regenerating...", e)
                certFile.delete()
                keyFile.delete()
            }
        }

        // Generate new 2048-bit RSA key pair
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        // Generate self-signed X.509 certificate valid for 10 years
        val now = System.currentTimeMillis()
        val startDate = Date(now - 24 * 60 * 60 * 1000L) // Yesterday
        val endDate = Date(now + 10L * 365 * 24 * 60 * 60 * 1000L) // +10 years
        val serial = BigInteger.valueOf(System.currentTimeMillis())

        val subject = X500Name("CN=$ALIAS")
        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            startDate,
            endDate,
            subject,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert = JcaX509CertificateConverter().getCertificate(certHolder)

        // Save to private storage
        FileOutputStream(certFile).use { it.write(cert.encoded) }
        FileOutputStream(keyFile).use { it.write(keyPair.private.encoded) }

        cachedKeyPair = keyPair
        cachedCertificate = cert
        Log.d(TAG, "Generated and saved new Android TV TLS certificate and key pair.")
        return Pair(keyPair, cert)
    }
}
