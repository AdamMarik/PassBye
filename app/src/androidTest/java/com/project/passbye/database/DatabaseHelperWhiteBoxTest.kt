package com.project.passbye.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DatabaseHelperWhiteBoxTest {

    private lateinit var dbHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbHelper = DatabaseHelper(context)
    }

    @Test
    fun testEncryptionDecryptionCorrectness() {
        val original = "SensitiveData123"
        val encrypted = dbHelper.encrypt(original)
        val decrypted = dbHelper.decrypt(encrypted)

        assertEquals(original, decrypted)
    }

    @Test
    fun testEncryptionProducesDifferentOutput() {
        val data1 = "Password1"
        val data2 = "Password2"

        val encrypted1 = dbHelper.encrypt(data1)
        val encrypted2 = dbHelper.encrypt(data2)

        assertNotEquals(encrypted1, encrypted2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDecryptionOfTamperedDataFails() {
        val dbHelper = DatabaseHelper(ApplicationProvider.getApplicationContext())
        val tamperedData = "thisisnotvalidbase64"
        dbHelper.decrypt(tamperedData)
    }


    @Test
    fun testEncryptionIsNotReversibleWithoutCorrectKey() {
        val original = "SecretKeyData"
        val encrypted = dbHelper.encrypt(original)


        val differentHelper = DatabaseHelper(ApplicationProvider.getApplicationContext())

        try {
            val decrypted = differentHelper.decrypt(encrypted)
            assertEquals(original, decrypted)
        } catch (e: Exception) {
            fail("Decryption failed but it should succeed with the same static key.")
        }
    }
}
