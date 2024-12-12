package com.project.passbye.activities

import android.os.AsyncTask
import android.util.Log
import java.net.InetAddress

class PingTool {

    fun pingHost(host: String) {
        PingTask().execute(host)
    }

    private class PingTask : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val host = params[0] ?: return "Host not found"
            try {
                val inetAddress = InetAddress.getByName(host)
                return if (inetAddress.isReachable(5000)) {
                    "Ping to $host successful!"
                } else {
                    "Ping to $host failed."
                }
            } catch (e: Exception) {
                Log.e("PingTool", "Error pinging host", e)
                return "Error pinging host"
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.d("PingTool", result ?: "No result")
        }
    }
}
