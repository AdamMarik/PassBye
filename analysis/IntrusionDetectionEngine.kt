package com.project.passbye.analysis

object IntrusionDetectionEngine {

    fun detectSsidSpoofing(ssidMap: MutableMap<String, MutableSet<String>>, ssid: String, bssid: String): String? {
        val bssids = ssidMap.getOrPut(ssid) { mutableSetOf() }
        bssids.add(bssid)
        return if (bssids.size > 2) {
            "⚠️ SSID Spoofing detected: '$ssid' broadcast from multiple BSSIDs (${bssids.joinToString()})"
        } else null
    }

    fun assessPortRisk(port: Int): String? {
        return when (port) {
            21 -> "FTP (cleartext login)"
            23 -> "Telnet (unencrypted)"
            139 -> "NetBIOS (LAN attack surface)"
            445 -> "SMB (wormable: EternalBlue)"
            3389 -> "RDP (remote desktop exposed)"
            else -> null
        }
    }
}