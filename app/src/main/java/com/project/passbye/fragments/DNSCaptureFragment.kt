package com.project.passbye.fragments

import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.project.passbye.R
import kotlinx.coroutines.*
import java.net.*

class DNSCaptureFragment : Fragment() {

    private lateinit var logView: TextView
    private lateinit var startBtn: Button
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running = false
    private var socket: DatagramSocket? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dns_capture, container, false)
        logView = view.findViewById(R.id.intrusion_log_output)
        startBtn = view.findViewById(R.id.start_monitor_btn)

        startBtn.setOnClickListener {
            if (!running) startDnsMonitor() else stopDnsMonitor()
        }

        return view
    }

    private fun startDnsMonitor() {
        running = true
        startBtn.text = "Stop Monitor"
        logView.text = "🔍 Monitoring UDP traffic for DNS spoofing...\n"
        scrollLogToBottom()
        startMonitoringDnsSpoofing()
    }

    private fun stopDnsMonitor() {
        running = false
        startBtn.text = "Start Monitor"
        scope.cancel()
        socket?.close()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        logView.append("⏹️ DNS monitoring stopped.\n")
        scrollLogToBottom()
    }

    private fun startMonitoringDnsSpoofing() {
        scope.launch {
            try {
                val port = 2100 // or any unused port
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    soTimeout = 2000
                    bind(InetSocketAddress("0.0.0.0", port))
                }

                val buffer = ByteArray(1024)
                withContext(Dispatchers.Main) {
                    logView.append("📡 Listening for spoofed DNS packets on 192.168.50.38:$port\n")
                    scrollLogToBottom()
                }

                while (running) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)

                        val payload = String(packet.data, 0, packet.length)
                        val sourceIp = packet.address.hostAddress
                        val sourcePort = packet.port

                        withContext(Dispatchers.Main) {
                            if (payload.contains("spoofed", ignoreCase = true)) {
                                logView.append("⚠️ Spoofed DNS from $sourceIp:$sourcePort\n")
                            } else {
                                logView.append("✅ Packet from $sourceIp:$sourcePort\n")
                            }
                            scrollLogToBottom()
                        }

                    } catch (e: SocketTimeoutException) {

                    }
                }
            } catch (e: Exception) {
                Log.e("DNSCapture", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    logView.append("❌ Error: ${e.localizedMessage}\n")
                    scrollLogToBottom()
                }
            }
        }
    }

    private fun scrollLogToBottom() {
        (logView.parent as? ScrollView)?.post {
            (logView.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopDnsMonitor()
    }
}
