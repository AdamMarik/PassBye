package com.project.passbye.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.passbye.R
import java.io.FileInputStream
import java.net.InetAddress

class PacketCaptureService : VpnService() {
    private val TAG = "PacketCaptureService"
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var captureThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            try {
                setupVpn()
                startPacketCapture()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting service: ${e.message}")
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
        builder.setSession("PassBye Packet Sniffer")
        builder.addAddress("10.0.0.2", 24) // VPN Gateway
        builder.addRoute("0.0.0.0", 0) // Route all traffic through VPN
        builder.addDnsServer("8.8.8.8")
        builder.allowFamily(OsConstants.AF_INET) // IPv4 traffic

        vpnInterface = builder.establish() ?: throw IllegalStateException("Failed to establish VPN")
        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "PACKET_CAPTURE_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Packet Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Packet Capture Service")
            .setContentText("Capturing network packets...")
            .setSmallIcon(R.drawable.ic_vpn) // Ensure this drawable exists
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun startPacketCapture() {
        isRunning = true
        val fileDescriptor = vpnInterface?.fileDescriptor ?: throw IllegalStateException("No VPN interface")
        val inputStream = FileInputStream(fileDescriptor)

        captureThread = Thread {
            try {
                val buffer = ByteArray(32767)
                while (isRunning) {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        val packet = buffer.copyOfRange(0, length)
                        val packetInfo = processPacket(packet)
                        sendPacketBroadcast(packetInfo)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing packets: ${e.message}")
            } finally {
                inputStream.close()
                stopSelf()
            }
        }
        captureThread?.start()
    }

    private fun processPacket(packet: ByteArray): String {
        return try {
            val ipHeader = packet.copyOfRange(0, 20)
            val sourceIP = InetAddress.getByAddress(ipHeader.sliceArray(12..15)).hostAddress
            val destIP = InetAddress.getByAddress(ipHeader.sliceArray(16..19)).hostAddress
            "Source: $sourceIP, Destination: $destIP, Length: ${packet.size}"
        } catch (e:Exception) {
            Log.e(TAG, "Error processing packet: ${e.message}")
            "Error processing packet"
        }
    }

    private fun sendPacketBroadcast(data: String) {
        val intent = Intent("LIVE_PACKET_UPDATE")
        intent.putExtra("packet_data", data)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        isRunning = false
        captureThread?.interrupt()
        vpnInterface?.close()
        vpnInterface = null
        Log.d(TAG, "Service destroyed and VPN closed")
        super.onDestroy()
    }
}