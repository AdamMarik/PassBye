package com.project.passbye.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.project.passbye.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
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
            val ipAddress = ipInputField.text.toString().trim()
            if (ipAddress.isNotEmpty()) {
                ping(ipAddress)
            } else {
                resultTextView.text = "Please enter an IP address or hostname."
            }
        }

        return rootView
    }

    private fun ping(ipAddress: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = performPing(ipAddress)

            withContext(Dispatchers.Main) {
                resultTextView.text = result
            }
        }
    }

    private fun performPing(ipAddress: String): String {
        return try {
            val address = InetAddress.getByName(ipAddress)
            if (!address.isReachable(2000)) {
                return "Request timed out."
            }

            val process = Runtime.getRuntime().exec("/system/bin/ping -c 4 $ipAddress")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            process.waitFor()
            output.toString()
        } catch (e: IOException) {
            "Ping Failed: ${e.message}"
        } catch (e: InterruptedException) {
            "Ping Interrupted: ${e.message}"
        }
    }
}
