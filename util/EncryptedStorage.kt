package com.project.passbye.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_passwords",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassword(username: String, domain: String, siteUsername: String, password: String) {
        val key = "${username}|${domain}|${siteUsername}"
        sharedPrefs.edit().putString(key, password).apply()
    }

    fun loadPasswordsForUser(username: String): Map<String, String> {
        return sharedPrefs.all.filterKeys { it.startsWith("$username|") }
            .mapValues { it.value as String }
    }

    fun deletePassword(username: String, domain: String, siteUsername: String) {
        val key = "${username}|${domain}|${siteUsername}"
        sharedPrefs.edit().remove(key).apply()
    }

    fun clearAll() {
        sharedPrefs.edit().clear().apply()
    }
}
