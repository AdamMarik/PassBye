package com.project.passbye.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.mindrot.jbcrypt.BCrypt

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "PassBye.db"
        const val DATABASE_VERSION = 5

        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_DISPLAY_PASSWORD = "display_password"
        const val COLUMN_FAILED_ATTEMPTS = "failed_attempts"
        const val COLUMN_LAST_FAILED = "last_failed_attempt"

        private val SECRET_KEY: SecretKey = generateKey()

        private fun generateKey(): SecretKey {
            val keyBytes = "16ByteSecretKey!".toByteArray()
            return SecretKeySpec(keyBytes, "AES")
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsersTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_USERNAME TEXT NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_DISPLAY_PASSWORD TEXT NOT NULL,
                $COLUMN_FAILED_ATTEMPTS INTEGER DEFAULT 0,
                $COLUMN_LAST_FAILED INTEGER DEFAULT 0
            );
        """.trimIndent()
        db?.execSQL(createUsersTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 5) {
            db?.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_DISPLAY_PASSWORD TEXT")
        }
    }

    fun insertUser(email: String, username: String, password: String): Boolean {
        val db = writableDatabase

        if (checkEmailExists(email)) {
            db.close()
            return false
        }

        val encryptedUsername = encrypt(username)
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val displayPassword = encrypt(password)

        return try {
            val sql = "INSERT INTO $TABLE_USERS ($COLUMN_EMAIL, $COLUMN_USERNAME, $COLUMN_PASSWORD, $COLUMN_DISPLAY_PASSWORD) VALUES (?, ?, ?, ?)"
            val stmt = db.compileStatement(sql)
            stmt.bindString(1, email)
            stmt.bindString(2, encryptedUsername)
            stmt.bindString(3, hashedPassword)
            stmt.bindString(4, displayPassword)
            stmt.executeInsert()
            true
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error inserting user: ${e.message}")
            false
        } finally {
            db.close()
        }
    }

    fun verifyUser(username: String, password: String): Boolean {
        if (isAdmin(username, password)) return true // Admin always allowed

        val db = readableDatabase
        val encryptedUsername = encrypt(username)

        val cursor = db.rawQuery(
            "SELECT $COLUMN_PASSWORD, $COLUMN_FAILED_ATTEMPTS, $COLUMN_LAST_FAILED FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?",
            arrayOf(encryptedUsername)
        )

        if (cursor.moveToFirst()) {
            val hashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
            val failedAttempts = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAILED_ATTEMPTS))
            cursor.close()

            if (failedAttempts >= 5) {
                return false
            }

            return if (BCrypt.checkpw(password, hashedPassword)) {
                resetFailedAttempts(username)
                true
            } else {
                incrementFailedAttempts(username)
                false
            }
        }
        cursor.close()
        return false
    }

    fun getFailedAttempts(username: String): Int {
        if (username == "Admin") return 0

        val db = readableDatabase
        val encryptedUsername = encrypt(username)
        val cursor = db.rawQuery(
            "SELECT $COLUMN_FAILED_ATTEMPTS FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?",
            arrayOf(encryptedUsername)
        )
        var attempts = 0
        if (cursor.moveToFirst()) {
            attempts = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAILED_ATTEMPTS))
        }
        cursor.close()
        db.close()
        return attempts
    }

    private fun incrementFailedAttempts(username: String) {
        if (username == "Admin") return

        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_USERS SET $COLUMN_FAILED_ATTEMPTS = $COLUMN_FAILED_ATTEMPTS + 1, $COLUMN_LAST_FAILED = ? WHERE $COLUMN_USERNAME = ?",
            arrayOf(System.currentTimeMillis(), encrypt(username))
        )
        db.close()
    }

    private fun resetFailedAttempts(username: String) {
        val db = writableDatabase
        db.execSQL(
            "UPDATE $TABLE_USERS SET $COLUMN_FAILED_ATTEMPTS = 0 WHERE $COLUMN_USERNAME = ?",
            arrayOf(encrypt(username))
        )
        db.close()
    }

    // --- These methods were missing before (Restored) ---

    fun getUserDetails(username: String): UserDetails? {
        val db = readableDatabase
        val encryptedUsername = encrypt(username)

        val cursor = db.rawQuery(
            "SELECT $COLUMN_EMAIL, $COLUMN_USERNAME, $COLUMN_DISPLAY_PASSWORD FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?",
            arrayOf(encryptedUsername)
        )

        return if (cursor.moveToFirst()) {
            val email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
            val encryptedUser = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
            val encryptedDisplayPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISPLAY_PASSWORD))
            cursor.close()

            val decryptedUsername = decrypt(encryptedUser)
            val decryptedPassword = decrypt(encryptedDisplayPassword)

            UserDetails(email, decryptedUsername, decryptedPassword)
        } else {
            cursor.close()
            null
        }
    }

    fun updatePassword(username: String, newPassword: String): Boolean {
        val db = writableDatabase
        val encryptedUsername = encrypt(username)
        val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        val encryptedDisplayPassword = encrypt(newPassword)

        return try {
            val sql = "UPDATE $TABLE_USERS SET $COLUMN_PASSWORD = ?, $COLUMN_DISPLAY_PASSWORD = ? WHERE $COLUMN_USERNAME = ?"
            val stmt = db.compileStatement(sql)
            stmt.bindString(1, hashedPassword)
            stmt.bindString(2, encryptedDisplayPassword)
            stmt.bindString(3, encryptedUsername)
            stmt.executeUpdateDelete() > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error updating password: ${e.message}")
            false
        } finally {
            db.close()
        }
    }

    internal fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY)
        return Base64.encodeToString(cipher.doFinal(data.toByteArray()), Base64.DEFAULT)
    }

    internal fun decrypt(data: String): String {
        return try {
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY)
            String(cipher.doFinal(Base64.decode(data, Base64.DEFAULT)))
        } catch (e: Exception) {
            throw IllegalArgumentException("Decryption failed", e)
        }
    }

    data class UserDetails(
        val email: String,
        val username: String,
        val decryptedPassword: String
    )

    fun unlockUser(username: String): Boolean {
        val db = writableDatabase
        return try {
            db.execSQL(
                "UPDATE $TABLE_USERS SET $COLUMN_FAILED_ATTEMPTS = 0 WHERE $COLUMN_USERNAME = ?",
                arrayOf(encrypt(username))
            )
            true
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    fun deleteUser(username: String): Boolean {
        val db = writableDatabase
        return try {
            db.execSQL(
                "DELETE FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?",
                arrayOf(encrypt(username))
            )
            true
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    fun getAllUsers(): List<String> {
        val db = readableDatabase
        val userList = mutableListOf<String>()
        val cursor = db.rawQuery("SELECT $COLUMN_USERNAME FROM $TABLE_USERS", null)
        while (cursor.moveToNext()) {
            userList.add(decrypt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))))
        }
        cursor.close()
        db.close()
        return userList
    }

    fun checkEmailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", arrayOf(email))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun isAdmin(username: String, password: String): Boolean {
        return username == "Admin" && password == "Admin_4002!"
    }
}
