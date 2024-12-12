package com.project.passbye.fragments

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.passbye.R
import java.io.IOException
import java.net.InetAddress

class PingToolFragment : Fragment(R.layout.fragment_ping_tool) {

    private lateinit var ipInputField: EditText
    private lateinit var pingButton: Button
    private lateinit var resultTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_ping_tool, container, false)

        ipInputField = rootView.findViewById(R.id.ip_input_field)
        pingButton = rootView.findViewById(R.id.ping_button)
        resultTextView = rootView.findViewById(R.id.result_textview)

        pingButton.setOnClickListener {
            val ipAddress = ipInputField.text.toString()
            if (ipAddress.isNotEmpty()) {
                ping(ipAddress)
            } else {
                resultTextView.text = "Please enter an IP address."
            }
        }

        return rootView
    }

    private fun ping(ipAddress: String) {
        PingTask(resultTextView).execute(ipAddress)
    }

    private class PingTask(val resultTextView: TextView) : AsyncTask<String, String, String>() {

        override fun doInBackground(vararg params: String?): String {
            val ipAddress = params[0] ?: return "Invalid IP address"

            try {
                val address = InetAddress.getByName(ipAddress)
                val pingCount = 4
                val sb = StringBuilder()

                for (i in 1..pingCount) {
                    val pingResult = address.isReachable(1000)
                    val result = if (pingResult) {
                        "Reply from $ipAddress: bytes=32 time=20ms TTL=128"
                    } else {
                        "Request timed out."
                    }
                    sb.append(result).append("\n")
                    publishProgress(sb.toString()) // Update TextView with each ping result
                    Thread.sleep(1000) // Wait before sending the next ping
                }
                return sb.toString()
            } catch (e: IOException) {
                return "Ping Failed: ${e.message}"
            }
        }

        override fun onProgressUpdate(vararg values: String?) {
            values[0]?.let { resultTextView.text = it } // Update the UI with each progress
        }

        override fun onPostExecute(result: String) {
            resultTextView.text = result // Final result after all pings
        }
    }
}
