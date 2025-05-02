package com.project.passbye.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.project.passbye.R

class ScannerFragment : Fragment(R.layout.fragment_scanner) {

    private lateinit var toolSpinner: Spinner
    private lateinit var toolsContainer: FrameLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_scanner, container, false)

        toolSpinner = rootView.findViewById(R.id.tool_spinner)
        toolsContainer = rootView.findViewById(R.id.tools_container)

        val tools = listOf("Ping Tool", "DNS / IP Lookup", "WiFi Scanner", "WiFi Safety Check", "Speed Test", "IP Tracker", "Packet Tracer", "Port Scanner", "SSID Scanner", "OS Scanner", "DNS Anomaly Detection", "OS Anomaly Detection")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tools)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        toolSpinner.adapter = adapter

        toolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                loadTool(position)
            }
            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        return rootView
    }

    private fun loadTool(position: Int) {
        toolsContainer.removeAllViews()
        val fragment = when (position) {
            0 -> PingToolFragment()
            1 -> LookupFragment()
            2 -> WifiScannerFragment()
            3 -> WifiSafetyFragment()
            4 -> SpeedTestFragment()
            5 -> NetworkScannerFragment()
            6 -> PacketCaptureFragment()
            7 -> PortScanFragment()
            8 -> SSIDCaptureFragment()
            9 -> TTLOSCaptureFragment()
            10 -> DNSCaptureFragment()
            11 -> TTLOSDetectionFragment()

            else -> null
        }

        fragment?.let {
            childFragmentManager.beginTransaction().replace(R.id.tools_container, it).commit()
        }
    }
}
