package com.project.passbye.analysis

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket

class DnsSnifferService : Service() {

    companion object {
        val dnsLogLiveData = MutableLiveData<String>()
        private const val TAG = "DnsSnifferService"
    }

    private var socket: DatagramSocket? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            startListening()
        }
        return START_STICKY
    }

    private fun startListening() {
        try {
            socket = DatagramSocket(2100)
            dnsLogLiveData.postValue("📡 Listening for spoofed DNS on port ${socket!!.localPort}")

            val buffer = ByteArray(512)
            while (!socket!!.isClosed) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket!!.receive(packet)

                val message = String(packet.data, 0, packet.length)
                val senderIp = packet.address.hostAddress
                val senderPort = packet.port

                if (message.contains("spoofed", ignoreCase = true)) {
                    dnsLogLiveData.postValue("⚠️ Spoofed DNS from $senderIp:$senderPort")
                } else {
                    dnsLogLiveData.postValue("✅ UDP Packet from $senderIp:$senderPort")
                }
            }
        } catch (e: Exception) {
            dnsLogLiveData.postValue("❌ Error: ${e.message}")
            Log.e(TAG, "Socket error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.close()
        serviceScope.cancel()
        dnsLogLiveData.postValue("🛑 DNS Sniffer stopped.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
