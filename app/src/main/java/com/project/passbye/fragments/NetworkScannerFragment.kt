package com.project.passbye.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.passbye.R
import com.project.passbye.activities.DeviceAdapter
import com.project.passbye.activities.LocalNetworkScanner

class NetworkScannerFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView

    private val devices = mutableListOf<Pair<String, String>>()
    private val adapter = DeviceAdapter(devices)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        statusTextView.text = "Scanning..."

        val scanner = LocalNetworkScanner()
        val subnet = "192.168.50"

        scanner.scanLocalNetwork(
            subnet,
            onDeviceFound = { ip, hostName ->
                requireActivity().runOnUiThread {
                    devices.add(Pair(ip, hostName))
                    adapter.notifyDataSetChanged()
                }
            },
            onScanCompleted = {
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    statusTextView.text = "Scan completed. ${devices.size} devices found."
                }
            }
        )
    }
}
