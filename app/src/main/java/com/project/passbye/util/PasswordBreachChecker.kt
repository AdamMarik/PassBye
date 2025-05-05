package com.project.passbye.util

import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest

object PasswordBreachChecker {

    private val client = OkHttpClient()

    fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }.uppercase()
    }

    suspend fun isPasswordPwned(password: String): Boolean {
        val sha1Hash = sha1(password)
        val prefix = sha1Hash.take(5)
        val suffix = sha1Hash.drop(5)

        val url = "https://api.pwnedpasswords.com/range/$prefix"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PassByeApp")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return false

        val responseBody = response.body?.string() ?: return false

        return responseBody.lineSequence().any {
            val (hashSuffix, count) = it.split(":")
            hashSuffix.equals(suffix, ignoreCase = true)
        }
    }
}
