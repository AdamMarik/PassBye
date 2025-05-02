package com.project.passbye.fragments

import android.net.wifi.WifiManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.project.passbye.R
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class TTLOSCaptureFragment : Fragment() {

    private lateinit var logText: TextView
    private lateinit var scanButton: Button
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_ttl_os_capture, container, false)

        logText = view.findViewById(R.id.ttl_log_output)
        scanButton = view.findViewById(R.id.ttl_scan_button)

        scanButton.setOnClickListener {
            scanButton.isEnabled = false
            logText.text = "🎯 Scanning local subnet for TTL OS detection...\n\n"
            startTTLDetection()
        }

        return view
    }

    private fun startTTLDetection() {
        scope.launch {
            val wifiManager = requireContext().applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            val subnet = String.format("%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff)

            var detected = 0
            val maxIPs = 255
            val dispatcher = newFixedThreadPoolContext(50, "PingPool")

            val jobs = (1..maxIPs).map { i ->
                async(dispatcher) {
                    val target = "$subnet.$i"
                    try {
                        val process = Runtime.getRuntime().exec("ping -c 1 -W 1 $target")
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        val output = reader.readText()
                        val ttl = Regex("ttl=(\\d+)").find(output)?.groupValues?.get(1)?.toIntOrNull()
                        val osGuess = guessOSFromTTL(ttl)

                        if (ttl != null) {
                            withContext(Dispatchers.Main) {
                                detected++
                                logText.append("IP: $target | TTL: $ttl | OS: $osGuess\n")
                                scrollLogToBottom()
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            jobs.awaitAll()

            withContext(Dispatchers.Main) {
                logText.append("\n✅ Scan complete. Devices fingerprinted: $detected\n")
                scanButton.isEnabled = true
            }

        }
    }

    private fun guessOSFromTTL(ttl: Int?): String {
        return when (ttl) {
            null -> "Unknown"
            in 0..64 -> "Linux/macOS"
            in 65..128 -> "Windows"
            in 129..255 -> "Cisco/Network Device"
            else -> "Unknown"
        }
    }

    private fun scrollLogToBottom() {
        (logText.parent as? ScrollView)?.post {
            (logText.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }
}
