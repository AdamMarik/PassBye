package com.project.passbye.fragments

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.project.passbye.R
import com.project.passbye.vpn.PacketCaptureVpnService

class PacketCaptureFragment : Fragment(R.layout.fragment_packet_capture) {

    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var outputView: TextView

    private val logLines = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startBtn = view.findViewById(R.id.button_start_capture)
        stopBtn = view.findViewById(R.id.button_stop_capture)
        outputView = view.findViewById(R.id.textview_output)

        startBtn.setOnClickListener { startVpn() }
        stopBtn.setOnClickListener { stopVpn() }

        PacketCaptureVpnService.livePacketData.observe(viewLifecycleOwner) {
            it?.let { packet ->
                logLines.add(packet)
                outputView.text = logLines.joinToString("\n\n")
            }
        }
    }

    private fun startVpn() {
        val intent = VpnService.prepare(requireContext())
        if (intent != null) startActivityForResult(intent, 100)
        else requireActivity().startForegroundService(Intent(requireContext(), PacketCaptureVpnService::class.java))
    }

    private fun stopVpn() {
        com.project.passbye.vpn.VpnServiceHelper.vpnService?.stopVpn()
    }
}
