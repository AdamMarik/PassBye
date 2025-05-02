package com.project.passbye.fragments

import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.project.passbye.R
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class TTLOSDetectionFragment : Fragment() {

    private lateinit var logText: TextView
    private lateinit var toggleButton: Button
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var running = false

    private val ttlMap = ConcurrentHashMap<String, MutableSet<Int>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_anomaly_detection, container, false)
        logText = view.findViewById(R.id.anomaly_log_text)
        toggleButton = view.findViewById(R.id.anomaly_scan_button)

        toggleButton.setOnClickListener {
            if (!running) startScan() else stopScan()
        }

        return view
    }

    private fun startScan() {
        running = true
        toggleButton.text = "Stop Anomaly Scan"
        logText.text = "🚨 Live anomaly detection started...\n"
        ttlMap.clear()

        scope.launch {
            val subnet = getSubnet() ?: return@launch

            while (running) {
                (1..254).forEach { host ->
                    val ip = "$subnet.$host"
                    launch {
                        val ttl = pingAndGetTTL(ip)
                        ttl?.let { analyzeTTL(ip, it) }
                    }
                }
                delay(3000) // scan every 3 seconds
            }
        }
    }

    private fun stopScan() {
        running = false
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        toggleButton.text = "Start Anomaly Scan"
        logText.append("✅ Scan stopped. Devices monitored: ${ttlMap.size}\n")
    }

    private suspend fun pingAndGetTTL(ip: String): Int? {
        return try {
            val proc = Runtime.getRuntime().exec("ping -c 1 -W 1 $ip")
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val output = reader.readText()
            proc.waitFor()
            Regex("ttl=(\\d+)").find(output)?.groupValues?.get(1)?.toInt()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun analyzeTTL(ip: String, ttl: Int) {
        val existingTTLs = ttlMap.getOrPut(ip) { mutableSetOf() }

        if (existingTTLs.add(ttl)) {
            if (existingTTLs.size > 1) {
                withContext(Dispatchers.Main) {
                    logText.append("⚠️ Anomaly Detected at $ip → TTLs: ${existingTTLs.sorted()}\n")
                    scrollToBottom()
                }
            } else {
                withContext(Dispatchers.Main) {
                    logText.append("📥 Device at $ip responded with TTL: $ttl\n")
                    scrollToBottom()
                }
            }
        }
    }

    private fun getSubnet(): String? {
        val wifiManager = context?.applicationContext?.getSystemService(WIFI_SERVICE) as? WifiManager ?: return null
        val ip = wifiManager.connectionInfo.ipAddress
        return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}"
    }

    private fun scrollToBottom() {
        (logText.parent as? ScrollView)?.post {
            (logText.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopScan()
    }
}
