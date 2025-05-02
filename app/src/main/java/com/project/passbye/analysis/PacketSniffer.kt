package com.project.passbye.forwarder

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.project.passbye.analysis.PacketParsers
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.InetAddress

class PacketSniffer(
    private val input: InputStream,
    private val livePacketData: MutableLiveData<String>
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val buffer = ByteArray(32767)

    fun start() {
        scope.launch {
            while (isActive) {
                try {
                    val length = input.read(buffer)
                    if (length > 0) {
                        val packet = buffer.copyOf(length)
                        parse(packet)
                    }
                } catch (e: Exception) {
                    Log.e("PacketSniffer", "❌ Error reading packet", e)
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    private fun parse(packet: ByteArray) {
        if ((packet[0].toInt() shr 4) != 4) return
        val protocol = packet[9].toInt() and 0xFF
        val ipHeaderLength = (packet[0].toInt() and 0x0F) * 4
        val dstIp = InetAddress.getByAddress(packet.sliceArray(16..19)).hostAddress

        when (protocol) {
            17 -> { // UDP (DNS)
                val dstPort = ((packet[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or (packet[ipHeaderLength + 3].toInt() and 0xFF)
                if (dstPort == 53) {
                    val payloadOffset = ipHeaderLength + 8
                    val payload = packet.copyOfRange(payloadOffset, packet.size)
                    PacketParsers.extractDnsQuery(payload)?.let {
                        livePacketData.postValue("🌍 DNS Query: $it → IP: $dstIp")
                    }
                }
            }
            6 -> { // TCP (SNI)
                val tcpOffset = ipHeaderLength
                val tcpHeaderLen = ((packet[tcpOffset + 12].toInt() shr 4) and 0xF) * 4
                val payloadOffset = ipHeaderLength + tcpHeaderLen
                if (packet.size > payloadOffset) {
                    val payload = packet.copyOfRange(payloadOffset, packet.size)
                    PacketParsers.extractSni(payload)?.let {
                        livePacketData.postValue("🔒 HTTPS SNI: $it → IP: $dstIp")
                    }
                }
            }
        }
    }
}
