package com.project.passbye.activities

import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log

class NetworkSniffer : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.d("NetworkSniffer", "Request: ${request.url}")

        // Proceed with the request
        val response = chain.proceed(request)

        // Log the response body (be cautious with large bodies)
        response.body?.let {
            Log.d("NetworkSniffer", "Response: ${it.string()}")
        }

        return response
    }
}
