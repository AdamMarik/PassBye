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

class SSIDCaptureFragment : Fragment() {

    private lateinit var detectionText: TextView
    private lateinit var startButton: Button
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val ssidMap = mutableMapOf<String, MutableSet<String>>()
    private val resultBuilder = StringBuilder()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ssid_capture, container, false)
        detectionText = view.findViewById(R.id.intrusion_result_text)
        startButton = view.findViewById(R.id.start_detection_button)

        startButton.setOnClickListener {
            startButton.isEnabled = false
            detectionText.text = "🔍 Scanning for potential SSID Spoofing..."
            scanWifiSpoofing()
        }

        return view
    }

    private fun scanWifiSpoofing() {
        val wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        scope.launch {
            val success = wifiManager.startScan()
            delay(2000) // wait for scan to complete

            if (success) {
                val results = wifiManager.scanResults
                for (result in results) {
                    val ssid = result.SSID
                    val bssid = result.BSSID
                    val alert = IntrusionDetectionEngine.detectSsidSpoofing(ssidMap, ssid, bssid)
                    if (alert != null) {
                        resultBuilder.append("\n$alert\n")
                    }
                }
                withContext(Dispatchers.Main) {
                    detectionText.text = if (resultBuilder.isEmpty()) {
                        "✅ No spoofing detected."
                    } else resultBuilder.toString()
                    startButton.isEnabled = true
                }
            } else {
                withContext(Dispatchers.Main) {
                    detectionText.text = "❌ Failed to start WiFi scan."
                    startButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }
}
