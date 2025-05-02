package com.project.passbye.vpn

import android.app.*
import android.content.Intent
import android.net.*
import android.os.*
import android.system.OsConstants
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.project.passbye.forwarder.PacketSniffer
import kotlinx.coroutines.*
import java.io.FileInputStream

class PacketCaptureVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sniffer: PacketSniffer? = null

    companion object {
        val livePacketData = MutableLiveData<String>()
        private const val CHANNEL_ID = "vpn_channel"
        private const val NOTIFICATION_ID = 1
        private const val VPN_IPV4 = "10.0.0.2"
        private const val VPN_MTU = 1500
    }

    override fun onCreate() {
        super.onCreate()
        VpnServiceHelper.vpnService = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setupVpn()
        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
            .setSession("PassBye VPN")
            .setMtu(VPN_MTU)
            .addAddress(VPN_IPV4, 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networks = getSystemService(ConnectivityManager::class.java)?.allNetworks
            if (!networks.isNullOrEmpty()) {
                builder.setUnderlyingNetworks(networks)
            }
        }

        builder.allowFamily(OsConstants.AF_INET)
        vpnInterface = builder.establish()
        Log.i("PacketCaptureVpnService", "✅ VPN started and interface established.")

        startSniffer()
    }

    private fun startSniffer() {
        vpnInterface?.fileDescriptor?.let {
            val input = FileInputStream(it)
            sniffer = PacketSniffer(input, livePacketData)
            sniffer?.start()
        }
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    fun stopVpn() {
        sniffer?.stop()
        scope.cancel()
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PassBye VPN")
            .setContentText("Monitoring traffic")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "VPN Monitoring", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
