package com.project.passbye.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.*
import android.view.*
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.project.passbye.R
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.highlight.Highlight
import kotlin.random.Random

class WifiScannerFragment : Fragment() {

    private lateinit var wifiManager: WifiManager
    private lateinit var chart: LineChart
    private lateinit var btn2_4: Button
    private lateinit var btn5: Button
    private lateinit var btn6: Button
    private val handler = Handler(Looper.getMainLooper())
    private val scanInterval = 5000L
    private var selectedBand = Band.BAND_2_4
    private var lastResults: List<ScanResult> = emptyList()
    private val ssidColorMap = mutableMapOf<String, Int>()

    enum class Band { BAND_2_4, BAND_5, BAND_6 }

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                displayGraph(wifiManager.scanResults)
            } catch (_: SecurityException) {}
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_wifi_scanner, container, false)

        chart = view.findViewById(R.id.wifi_chart)
        btn2_4 = view.findViewById(R.id.btn_24ghz)
        btn5 = view.findViewById(R.id.btn_5ghz)
        btn6 = view.findViewById(R.id.btn_6ghz)

        btn2_4.setOnClickListener {
            selectedBand = Band.BAND_2_4
            updateButtonStyles()
        }

        btn5.setOnClickListener {
            selectedBand = Band.BAND_5
            updateButtonStyles()
        }

        btn6.setOnClickListener {
            selectedBand = Band.BAND_6
            updateButtonStyles()
        }

        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        } else {
            startWifiScan()
        }

        updateButtonStyles()
        setupChartListener()
        return view
    }

    private fun updateButtonStyles() {
        val active = resources.getColor(R.color.teal_700)
        val inactive = resources.getColor(R.color.darker_gray)

        btn2_4.setBackgroundColor(if (selectedBand == Band.BAND_2_4) active else inactive)
        btn5.setBackgroundColor(if (selectedBand == Band.BAND_5) active else inactive)
        btn6.setBackgroundColor(if (selectedBand == Band.BAND_6) active else inactive)
    }

    private fun startWifiScan() {
        requireContext().registerReceiver(scanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        handler.post(object : Runnable {
            override fun run() {
                try { wifiManager.startScan() } catch (_: SecurityException) {}
                handler.postDelayed(this, scanInterval)
            }
        })
    }

    private fun displayGraph(results: List<ScanResult>) {
        val dataSets = ArrayList<ILineDataSet>()
        lastResults = results

        val filtered = results.filter {
            when (selectedBand) {
                Band.BAND_2_4 -> it.frequency in 2400..2500
                Band.BAND_5 -> it.frequency in 4900..5900
                Band.BAND_6 -> it.frequency in 5925..7125
            }
        }

        filtered.forEach { result ->
            val channel = freqToChannel(result.frequency)
            val ssid = if (result.SSID.isBlank()) "(Hidden)" else result.SSID
            val signal = result.level.toFloat()

            val signalEntry = Entry(channel.toFloat(), signal)
            val conePoints = listOf(
                Entry(channel - 2f, -100f),
                signalEntry,
                Entry(channel + 2f, -100f)
            )

            val color = ssidColorMap.getOrPut(ssid) {
                Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
            }

            val dataSet = LineDataSet(conePoints, ssid)
            dataSet.color = color
            dataSet.fillColor = color
            dataSet.setDrawFilled(false)
            dataSet.setDrawCircles(true)
            dataSet.setCircleColor(color)
            dataSet.setDrawValues(true)
            dataSet.valueTextColor = android.graphics.Color.WHITE
            dataSet.valueTextSize = 10f
            dataSet.lineWidth = 2f

            dataSet.valueFormatter = object : ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    return if (entry == signalEntry) entry?.y?.toInt().toString() else ""
                }
            }

            dataSets.add(dataSet)
        }

        chart.data = LineData(dataSets)
        chart.axisLeft.axisMinimum = -100f
        chart.axisLeft.axisMaximum = -20f
        chart.axisLeft.textColor = android.graphics.Color.LTGRAY
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.xAxis.textColor = android.graphics.Color.LTGRAY
        chart.description.isEnabled = false
        chart.legend.textColor = android.graphics.Color.WHITE
        chart.invalidate()
    }

    private fun setupChartListener() {
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e == null || h == null) return

                val dataSet = chart.data.getDataSetByIndex(h.dataSetIndex)
                val ssid = dataSet.label
                val matched = lastResults.find { it.SSID == ssid || (it.SSID.isBlank() && ssid == "(Hidden)") }
                matched?.let { result ->
                    val details = """
                        SSID: ${if (result.SSID.isBlank()) "(Hidden)" else result.SSID}
                        BSSID: ${result.BSSID}
                        Frequency: ${result.frequency} MHz
                        Channel: ${freqToChannel(result.frequency)}
                        Signal: ${result.level} dBm
                        Encryption: ${result.capabilities}
                    """.trimIndent()

                    AlertDialog.Builder(requireContext())
                        .setTitle("Network Info")
                        .setMessage(details)
                        .setPositiveButton("Close", null)
                        .show()
                }
            }

            override fun onNothingSelected() {}
        })
    }

    private fun freqToChannel(freq: Int): Int = when (freq) {
        in 2412..2484 -> (freq - 2407) / 5
        in 5170..5825 -> (freq - 5000) / 5
        in 5935..7115 -> ((freq - 5955) / 5) + 1
        else -> -1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { requireContext().unregisterReceiver(scanReceiver) } catch (_: Exception) {}
        handler.removeCallbacksAndMessages(null)
    }
}

