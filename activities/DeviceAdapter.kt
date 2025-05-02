package com.project.passbye.activities

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.passbye.R

class DeviceAdapter(private val devices: List<Triple<String, String, String>>) :
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
        val (ip, host) = devices[position]
        holder.ipTextView.text = "IP: $ip"
        holder.hostNameTextView.text = "Hostname: $host"
    }

    override fun getItemCount(): Int = devices.size
}
