package com.project.passbye.fragments

import android.content.Intent
import android.net.Uri
import android.os.*
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.project.passbye.R
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class LookupFragment : Fragment() {

    private lateinit var inputField: EditText
    private lateinit var lookupButton: Button
    private lateinit var resultText: TextView
    private lateinit var openInBrowserButton: Button
    private var lastLookupUrl = ""
    private val scope = CoroutineScope(Dispatchers.IO)
    private val TAG = "LookupFragment"

    private val virusTotalApiKey = "69f31e6e03802c7f0887073d6203e7fb33e80476fbee642eba87edc549a3b2a4"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_lookup, container, false)

        inputField = view.findViewById(R.id.lookup_input)
        lookupButton = view.findViewById(R.id.lookup_button)
        resultText = view.findViewById(R.id.lookup_result)
        openInBrowserButton = view.findViewById(R.id.lookup_open_browser)

        openInBrowserButton.setOnClickListener {
            if (lastLookupUrl.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lastLookupUrl)))
            }
        }

        lookupButton.setOnClickListener {
            val query = inputField.text.toString().trim()
            if (query.isNotEmpty()) {
                resultText.text = "🔍 Looking up $query..."
                performLookup(query)
            }
        }

        return view
    }

    private fun performLookup(originalQuery: String) {
        scope.launch {
            try {
                Log.i(TAG, "Starting lookup for: $originalQuery")

                val resolvedIp = try {
                    if (originalQuery.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                        originalQuery
                    } else {
                        val ip = InetAddress.getByName(originalQuery).hostAddress
                        Log.i(TAG, "Resolved $originalQuery -> $ip")
                        ip
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to resolve domain to IP", e)
                    withContext(Dispatchers.Main) {
                        resultText.text = "❌ Failed to resolve domain to IP"
                    }
                    return@launch
                }

                val url = URL("https://ipwho.is/$resolvedIp")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val responseCode = conn.responseCode
                Log.i(TAG, "HTTP response code: $responseCode")
                if (responseCode != 200) throw Exception("Non-200 response from ipwho.is")

                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                if (!json.optBoolean("success", false)) {
                    Log.e(TAG, "❌ API returned unsuccessful result")
                    withContext(Dispatchers.Main) {
                        resultText.text = "❌ Lookup failed: API did not return valid data."
                    }
                    return@launch
                }

                val ip = json.optString("ip", resolvedIp)
                val rdns = json.optString("reverse", "N/A")
                val org = json.optString("org", "N/A")
                val country = json.optString("country", "N/A")
                val city = json.optString("city", "")
                val region = json.optString("region", "")
                val asn = json.optString("asn", "N/A")
                val isp = json.optJSONObject("connection")?.optString("isp", "N/A") ?: "N/A"

                lastLookupUrl = "https://www.virustotal.com/gui/search/$originalQuery"
                val riskText = getVirusTotalRisk(originalQuery)

                withContext(Dispatchers.Main) {
                    val resultInfo = """
                        🔍 Lookup Result:

                        ▫ IP Address: $ip
                        ▫ Reverse DNS: $rdns
                        ▫ ISP / Org: $org
                        ▫ Country: $country
                        ▫ Location: $city, $region, $country
                        ▫ ASN: $asn
                        ▫ ISP: $isp

                        🔗 Full Report:
                        $lastLookupUrl
                    """.trimIndent()

                    resultText.text = resultInfo
                    resultText.append("\n\n")

                    val color = when {
                        "Malicious: 0" in riskText && "Suspicious: 0" in riskText -> android.graphics.Color.GREEN
                        "Malicious: 0" in riskText -> android.graphics.Color.YELLOW
                        else -> android.graphics.Color.RED
                    }

                    val styled = SpannableString("🛡️ VirusTotal Risk:\n$riskText")
                    styled.setSpan(ForegroundColorSpan(color), styled.indexOf(riskText), styled.length, 0)
                    resultText.append(styled)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during lookup", e)
                withContext(Dispatchers.Main) {
                    resultText.text = "❌ Failed to fetch data. Check network or input."
                }
            }
        }
    }

    private fun getVirusTotalRisk(query: String): String {
        return try {
            val isIp = query.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))
            val endpoint = if (isIp)
                "https://www.virustotal.com/api/v3/ip_addresses/$query"
            else
                "https://www.virustotal.com/api/v3/domains/$query"

            val vtUrl = URL(endpoint)
            val vtConn = vtUrl.openConnection() as HttpURLConnection
            vtConn.requestMethod = "GET"
            vtConn.setRequestProperty("x-apikey", virusTotalApiKey)
            vtConn.connectTimeout = 5000
            vtConn.readTimeout = 5000

            val responseCode = vtConn.responseCode
            Log.i(TAG, "VirusTotal response code: $responseCode")

            if (responseCode != 200) {
                Log.e(TAG, "VirusTotal returned code $responseCode")
                return "Unknown (VT error: $responseCode)"
            }

            val response = vtConn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val stats = json.getJSONObject("data")
                .getJSONObject("attributes")
                .getJSONObject("last_analysis_stats")

            val malicious = stats.optInt("malicious", 0)
            val harmless = stats.optInt("harmless", 0)
            val suspicious = stats.optInt("suspicious", 0)

            "Malicious: $malicious | Suspicious: $suspicious | Harmless: $harmless"
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ VirusTotal lookup failed", e)
            "Unknown (VT error)"
        }
    }
}
