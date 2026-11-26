package com.monexplorateur.app.ftp

import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.net.Inet4Address
import java.net.NetworkInterface

object ServeurFtpManager {
    private var server: FtpServer? = null

    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours

    private val _adresseUrl = MutableStateFlow("")
    val adresseUrl: StateFlow<String> = _adresseUrl

    fun demarrer(port: Int = 2221, utilisateur: String = "android", motDePasse: String = "1234"): Result<String> {
        if (server != null && _enCours.value) {
            return Result.success(_adresseUrl.value)
        }
        return try {
            val factory = FtpServerFactory()
            val listenerFactory = ListenerFactory().apply {
                this.port = port
            }
            factory.addListener("default", listenerFactory.createListener())

            val user = BaseUser().apply {
                name = utilisateur
                password = motDePasse
                homeDirectory = Environment.getExternalStorageDirectory().absolutePath
                authorities = listOf<Authority>(WritePermission())
            }
            factory.userManager.save(user)

            val instance = factory.createServer()
            instance.start()
            server = instance

            val ip = obtenirIpLocale() ?: "127.0.0.1"
            val url = "ftp://$ip:$port"
            _adresseUrl.value = url
            _enCours.value = true
            Result.success(url)
        } catch (e: Exception) {
            _enCours.value = false
            Result.failure(e)
        }
    }

    fun arreter() {
        try {
            server?.stop()
        } catch (_: Exception) {
        } finally {
            server = null
            _enCours.value = false
            _adresseUrl.value = ""
        }
    }

    private fun obtenirIpLocale(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
