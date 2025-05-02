package com.project.passbye.activities

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.util.concurrent.Executors

class LocalNetworkScanner(private val context: Context) {
    private val executor = Executors.newFixedThreadPool(50)
    private val discoveredIps = mutableSetOf<String>()
    fun scanLocalNetwork(
        onDeviceFound: (String, String, String) -> Unit,
        onScanCompleted: () -> Unit
    ) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress
        val subnet = String.format("%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff)

        for (i in 1..254) {
            val host = "$subnet.$i"
            executor.execute {
                try {
                    val mac = getMacFromArp(host)
                    if (mac != null && mac != "00:00:00:00:00:00") {
                        if (discoveredIps.add(host)) {
                            onDeviceFound(host, "Unknown", mac)
                        }
                        return@execute
                    }

                    if (pingViaShell(host)) {
                        val address = InetAddress.getByName(host)
                        val hostName = try {
                            address.canonicalHostName ?: "Unknown"
                        } catch (e: Exception) {
                            "Unknown"
                        }
                        if (discoveredIps.add(host)) {
                            onDeviceFound(host, hostName, "Unavailable")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NetworkScanner", "Error scanning $host", e)
                }
            }
        }

        discoverMdnsDevices(context) { ip, name ->
            if (discoveredIps.add(ip)) {
                onDeviceFound(ip, name, "via mDNS")
            }
        }

        executor.shutdown()
        Thread {
            while (!executor.isTerminated) {
                Thread.sleep(100)
            }
            onScanCompleted()
        }.start()
    }

    private fun getMacFromArp(ip: String): String? {
        try {
            val reader = BufferedReader(FileReader("/proc/net/arp"))
            reader.readLine()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(Regex("\\s+"))
                if (parts.size >= 4 && parts[0] == ip) {
                    return parts[3]
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("NetworkScanner", "ARP read error", e)
        }
        return null
    }

    private fun pingViaShell(ip: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 1 $ip")
            val result = process.waitFor()
            result == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun discoverMdnsDevices(context: Context, onFound: (String, String) -> Unit) {
        try {
            val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            val discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}

                override fun onServiceFound(service: NsdServiceInfo) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onServiceResolved(resolved: NsdServiceInfo) {
                            val host = resolved.host.hostAddress ?: return
                            val name = resolved.serviceName
                            onFound(host, name)
                        }

                        override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {}
                    })
                }

                override fun onServiceLost(service: NsdServiceInfo) {}
            }

            nsdManager.discoverServices(
                "_services._dns-sd._udp.",
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e("NetworkScanner", "mDNS discovery failed", e)
        }
    }
}
