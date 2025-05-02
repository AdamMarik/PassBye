package com.project.passbye.fragments

import android.os.Bundle
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.passbye.R
import com.project.passbye.activities.DeviceAdapter
import com.project.passbye.activities.LocalNetworkScanner

class NetworkScannerFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView

    private val devices = mutableListOf<Triple<String, String, String>>()
    private val adapter = DeviceAdapter(devices)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_network_scanner, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        statusTextView = view.findViewById(R.id.statusTextView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        startNetworkScan()

        return view
    }

    private fun startNetworkScan() {
        progressBar.visibility = View.VISIBLE
        statusTextView.text = "🔍 Scanning..."

        val scanner = LocalNetworkScanner(requireContext())
        scanner.scanLocalNetwork(
            onDeviceFound = { ip, host, mac ->
                requireActivity().runOnUiThread {
                    devices.add(Triple(ip, host, mac))
                    adapter.notifyItemInserted(devices.size - 1)
                }
            },
            onScanCompleted = {
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    statusTextView.text = "✅ Scan complete. ${devices.size} devices found."
                }
            }
        )
    }
}
