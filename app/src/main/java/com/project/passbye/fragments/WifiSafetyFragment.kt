package com.project.passbye.fragments

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.project.passbye.R

class WifiSafetyFragment : Fragment() {

    private lateinit var scanButton: Button
    private lateinit var resultText: TextView
    private lateinit var wifiManager: WifiManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_wifi_safety, container, false)

        scanButton = view.findViewById(R.id.wifi_scan_button)
        resultText = view.findViewById(R.id.wifi_safety_result)
        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        scanButton.setOnClickListener {
            scanAndEvaluateWifi()
        }

        return view
    }

    private fun scanAndEvaluateWifi() {
        resultText.text = "🔍 Scanning WiFi networks...\n"
        wifiManager.startScan()
        val results = wifiManager.scanResults
        val connected = wifiManager.connectionInfo

        val builder = StringBuilder()

        builder.append("📡 Connected Network:\n")
        builder.append(getWifiInfoString(connected, results))
        builder.append("\n📶 Nearby Networks:\n")

        results.forEach {
            builder.append(getScanResultString(it))
        }

        resultText.text = builder.toString()
    }

    private fun getWifiInfoString(info: WifiInfo, results: List<ScanResult>): String {
        val ssid = info.ssid.removeSurrounding("\"")
        val bssid = info.bssid ?: "Unknown"
        val signalDbm = info.rssi
        val match = results.find { it.BSSID == bssid }
        val capabilities = match?.capabilities ?: "Unknown"
        val safety = evaluateSecurity(capabilities, ssid)

        return """
            ▫ SSID: $ssid
            ▫ BSSID: $bssid
            ▫ Signal: ${signalDbm} dBm
            ▫ Encryption: $capabilities
            ▫ Score: ${safety.first}/10
            ▫ Verdict: ${safety.second}
            ${"•".repeat(40)}
        """.trimIndent() + "\n"
    }

    private fun getScanResultString(result: ScanResult): String {
        val ssid = if (result.SSID.isBlank()) "(Hidden SSID)" else result.SSID
        val signalDbm = result.level
        val safety = evaluateSecurity(result.capabilities, ssid)

        return """
            🔸 SSID: $ssid
            ▫ Signal: ${signalDbm} dBm
            ▫ Encryption: ${result.capabilities}
            ▫ Score: ${safety.first}/10
            ▫ Verdict: ${safety.second}
            ${"•".repeat(40)}
        """.trimIndent() + "\n"
    }

    private fun evaluateSecurity(capabilities: String, ssid: String): Pair<Int, String> {
        var score = 0

        // Base score from encryption
        when {
            capabilities.contains("WPA3") -> score += 9
            capabilities.contains("WPA2") -> score += 7
            capabilities.contains("WPA") -> score += 5
            capabilities.contains("WEP") -> score += 2
            else -> score += 1 // Open network
        }

        // Penalties
        if (capabilities.contains("WPS")) score -= 2
        if (ssid.isBlank() || ssid == "(Hidden SSID)") score -= 2

        score = score.coerceIn(1, 10)

        // Verdicts
        val verdict = when {
            score >= 9 -> "\uD83D\uDD35 Excellent Security"
            score >= 7 -> "\uD83D\uDFE2 Good Security"
            score >= 4 -> "\uD83D\uDFE0 Fair Security"
            else -> "\uD83D\uDD34 Poor / Unsecured"
        }

        return score to verdict
    }
}
