package com.project.passbye.fragments

import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.project.passbye.R
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import javax.net.ssl.HttpsURLConnection

class SpeedTestFragment : Fragment() {

    private lateinit var testButton: Button
    private lateinit var resultText: TextView
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_speedtest, container, false)

        testButton = view.findViewById(R.id.start_speed_test_button)
        resultText = view.findViewById(R.id.speed_test_result)

        testButton.setOnClickListener {
            testSpeed()
        }

        return view
    }

    private fun testSpeed() {
        Log.i("SpeedTest", "⚡ Starting speed test")
        resultText.text = "Running speed test...\n"

        scope.launch {
            val ping = measurePing("8.8.8.8", 53)
            val downloadSpeed = measureDownloadSpeed("https://speed.cloudflare.com/__down?bytes=100000000")
            val uploadSpeed = measureUploadSpeed("https://speed.cloudflare.com/__up")


            withContext(Dispatchers.Main) {
                resultText.text = """
                    📶 Speed Test Result:
                    🕓 Ping: ${if (ping >= 0) "$ping ms" else "Failed"}
                    ⬇️ Download: ${if (downloadSpeed > 0) "%.2f Mbps".format(downloadSpeed) else "Failed"}
                    ⬆️ Upload: ${if (uploadSpeed > 0) "%.2f Mbps".format(uploadSpeed) else "Failed"}
                """.trimIndent()
            }
        }
    }

    private fun measurePing(host: String, port: Int): Long {
        return try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 3000)
            socket.close()
            val pingTime = System.currentTimeMillis() - start
            Log.i("SpeedTest", "✅ Ping success: $pingTime ms")
            pingTime
        } catch (e: IOException) {
            Log.e("SpeedTest", "❌ Ping failed", e)
            -1
        }
    }

    private fun measureDownloadSpeed(fileUrl: String): Double {
        return try {
            Log.i("SpeedTest", "⬇️ Starting download test from $fileUrl")
            val start = System.currentTimeMillis()
            val url = URL(fileUrl)
            val conn = url.openConnection()
            conn.connect()

            val input = BufferedInputStream(conn.getInputStream())
            val buffer = ByteArray(8192)
            var downloaded = 0
            val timeout = 8000

            while (System.currentTimeMillis() - start < timeout) {
                val read = input.read(buffer)
                if (read == -1) break
                downloaded += read
            }

            input.close()
            val duration = (System.currentTimeMillis() - start) / 1000.0
            val speed = (downloaded * 8) / (duration * 1_000_000)
            Log.i("SpeedTest", "✅ Download success: %.2f Mbps".format(speed))
            speed
        } catch (e: Exception) {
            Log.e("SpeedTest", "❌ Download failed", e)
            0.0
        }
    }

    private fun measureUploadSpeed(targetUrl: String): Double {
        return try {
            Log.i("SpeedTest", "⬆️ Starting upload test to $targetUrl")
            val testData = ByteArray(1024 * 1024) { 0 }

            val start = System.currentTimeMillis()

            val url = URL(targetUrl)
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.connectTimeout = 5000
            conn.readTimeout = 8000

            val out = BufferedOutputStream(conn.outputStream)
            out.write(testData)
            out.flush()
            out.close()

            conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val duration = (System.currentTimeMillis() - start) / 1000.0
            val mbps = (testData.size * 8) / (duration * 1_000_000)
            Log.i("SpeedTest", "✅ Upload success: %.2f Mbps".format(mbps))
            mbps
        } catch (e: Exception) {
            Log.e("SpeedTest", "❌ Upload failed", e)
            0.0
        }
    }

}
