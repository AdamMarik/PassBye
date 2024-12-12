package com.project.passbye.fragments

import com.project.passbye.activities.PacketCaptureService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.passbye.R

class PacketCaptureFragment : Fragment(R.layout.fragment_packet_capture) {
    private val VPN_REQUEST_CODE = 100

    private lateinit var textViewOutput: TextView
    private lateinit var startCaptureButton: Button
    private var isCapturing = false

    private val packetUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packetData = intent?.getStringExtra("packet_data")
            packetData?.let {
                requireActivity().runOnUiThread {
                    textViewOutput.append("$it\n")
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textViewOutput = view.findViewById(R.id.textview_output)
        startCaptureButton = view.findViewById(R.id.button_start_capture)

        startCaptureButton.setOnClickListener {
            if (isCapturing) {
                stopVpnService()
            } else {
                startVpnService()
            }
        }
    }

    private fun startVpnService() {
        val vpnIntent = VpnService.prepare(requireContext())
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
        } else {
            initiateVpnService()
        }
    }

    private fun initiateVpnService() {
        val intent = Intent(requireContext(), PacketCaptureService::class.java)
        requireContext().startForegroundService(intent)
        isCapturing = true
        startCaptureButton.text = "Stop Capturing"
    }

    private fun stopVpnService() {
        val intent = Intent(requireContext(), PacketCaptureService::class.java)
        requireContext().stopService(intent)
        isCapturing = false
        startCaptureButton.text = "Start Capturing"
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(packetUpdateReceiver, IntentFilter("LIVE_PACKET_UPDATE"),
            Context.RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(packetUpdateReceiver)
    }
}