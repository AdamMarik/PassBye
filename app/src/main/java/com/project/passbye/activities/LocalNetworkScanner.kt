package com.project.passbye.activities

import android.util.Log
import java.net.InetAddress
import java.util.concurrent.Executors

class LocalNetworkScanner {

    private val executor = Executors.newFixedThreadPool(10) // Use a thread pool for parallel scanning

    /**
     * Scans the local network for active devices.
     * @param subnet The subnet (e.g., "192.168.1") to scan.
     * @param onDeviceFound Callback invoked when a device is found.
     * @param onScanCompleted Callback invoked when the scan is completed.
     */
    fun scanLocalNetwork(
        subnet: String,
        onDeviceFound: (String, String) -> Unit,
        onScanCompleted: () -> Unit
    ) {
        for (i in 1..254) { // Scan addresses from .1 to .254
            val host = "$subnet.$i"
            executor.execute {
                try {
                    val address = InetAddress.getByName(host)
                    if (address.isReachable(100)) { // Check if the host is reachable (timeout = 100ms)
                        val hostName = try {
                            address.canonicalHostName // Attempt to resolve the hostname
                        } catch (e: Exception) {
                            "Unknown Host"
                        }
                        Log.d("LocalNetworkScanner", "Device found: $host ($hostName)")
                        onDeviceFound(host, hostName)
                    }
                } catch (e: Exception) {
                    Log.e("LocalNetworkScanner", "Error scanning device: $host", e)
                }
            }
        }

        // Wait for all tasks to complete
        executor.shutdown()
        Thread {
            while (!executor.isTerminated) {
                Thread.sleep(100)
            }
            onScanCompleted()
        }.start()
    }
}
