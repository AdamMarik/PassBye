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
import com.example.passbye.R

class ScannerFragment : Fragment(R.layout.fragment_scanner) {

    private lateinit var toolSpinner: Spinner
    private lateinit var toolsContainer: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_scanner, container, false)

        toolSpinner = rootView.findViewById(R.id.tool_spinner)
        toolsContainer = rootView.findViewById(R.id.tools_container)

        // Set up the spinner to select the tools
        val tools = listOf("Ping Tool", "IP Tracker", "Packet Tracer")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tools)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        toolSpinner.adapter = adapter

        // Set up the item selected listener for the spinner
        toolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                loadTool(position)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Handle case if no tool is selected
            }
        }

        return rootView
    }

    private fun loadTool(position: Int) {
        // Clear the current tool in the container
        toolsContainer.removeAllViews()

        // Load the selected tool fragment based on spinner position
        val fragment = when (position) {
            0 -> PingToolFragment()  // Ping Tool
            1 -> NetworkScannerFragment()  // IP Tracker
            2 -> PacketCaptureFragment()  // Packet Tracer
            else -> null
        }

        // Load the fragment if it's not null
        fragment?.let {
            childFragmentManager.beginTransaction()
                .replace(R.id.tools_container, it)
                .commit()
        }
    }
}
