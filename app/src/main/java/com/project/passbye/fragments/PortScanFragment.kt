package com.project.passbye.fragments

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.project.passbye.R
import com.project.passbye.analysis.IntrusionDetectionEngine
import kotlinx.coroutines.*
import java.net.*

class PortScanFragment : Fragment() {

    private lateinit var logText: TextView
    private lateinit var scanButton: Button
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val commonPorts = mapOf(
        21 to "FTP", 22 to "SSH", 23 to "Telnet", 53 to "DNS", 80 to "HTTP",
        110 to "POP3", 139 to "NetBIOS", 143 to "IMAP", 443 to "HTTPS", 445 to "SMB",
        3306 to "MySQL", 3389 to "RDP"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_port_scan, container, false)
        logText = view.findViewById(R.id.intrusion_log_text)
        scanButton = view.findViewById(R.id.start_scan_button)

        scanButton.setOnClickListener {
            scanButton.isEnabled = false
            logText.text = "🔍 Scanning network, please wait..."
            val subnet = getLocalSubnet(requireContext())
            startFastPortScan(subnet)
        }

        return view
    }

    private fun getLocalSubnet(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wifiManager.connectionInfo.ipAddress
        return String.format("%d.%d.%d", ipInt and 0xff, ipInt shr 8 and 0xff, ipInt shr 16 and 0xff)
    }

    private fun startFastPortScan(subnet: String) {
        scope.launch {
            val reportBuilder = StringBuilder()
            val results = mutableListOf<Pair<String, List<String>>>()
            val jobs = mutableListOf<Deferred<Unit>>()

            for (i in 1..254) {
                val ip = "$subnet.$i"
                val job = async {
                    val openPorts = mutableListOf<String>()
                    for ((port, service) in commonPorts) {
                        try {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress(ip, port), 200)
                                val risk = IntrusionDetectionEngine.assessPortRisk(port)
                                val vulnFlag = if (risk != null) "⚠️ $risk" else ""
                                openPorts.add(" - Port $port: OPEN ($service) $vulnFlag")
                            }
                        } catch (_: Exception) {}
                    }
                    if (openPorts.isNotEmpty()) {
                        results.add(Pair(ip, openPorts))
                    }
                }
                jobs.add(job)
            }

            jobs.awaitAll()

            reportBuilder.append("📡 Subnet scanned: $subnet.0/254\n")
            reportBuilder.append("✅ Scan complete. ${results.size} devices with open ports.\n\n")

            var totalVulns = 0
            for ((ip, ports) in results) {
                reportBuilder.append("🔍 $ip:\n")
                ports.forEach {
                    reportBuilder.append("$it\n")
                    if ("⚠️" in it) totalVulns++
                }
                reportBuilder.append("\n")
            }

            withContext(Dispatchers.Main) {
                reportBuilder.append("🔐 Total vulnerabilities flagged: $totalVulns\n")
                logText.text = reportBuilder.toString()
                scanButton.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }
}
