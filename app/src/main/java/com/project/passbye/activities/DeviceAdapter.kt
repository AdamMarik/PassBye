package com.project.passbye.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.passbye.R

class DeviceAdapter(private val devices: List<Pair<String, String>>) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ipTextView: TextView = view.findViewById(R.id.ipTextView)
        val hostNameTextView: TextView = view.findViewById(R.id.hostNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val (ip, hostName) = devices[position]
        holder.ipTextView.text = ip
        holder.hostNameTextView.text = hostName
    }

    override fun getItemCount(): Int = devices.size
}
