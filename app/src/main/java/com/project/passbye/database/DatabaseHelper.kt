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
        const val DATABASE_VERSION = 3
        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_FAILED_ATTEMPTS = "failed_attempts"
        const val COLUMN_LAST_FAILED = "last_failed_attempt"

        private val SECRET_KEY: SecretKey = generateKey()

        private fun generateKey(): SecretKey {
            val keyBytes = "16ByteSecretKey!".toByteArray()
            return SecretKeySpec(keyBytes, "AES")
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_USERNAME TEXT NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_FAILED_ATTEMPTS INTEGER DEFAULT 0,
                $COLUMN_LAST_FAILED INTEGER DEFAULT 0
            );
        """.trimIndent()
        db?.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun insertUser(email: String, username: String, password: String): Boolean {
        val db = writableDatabase
        val encryptedUsername = encrypt(username)
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

        return try {
            val sql = "INSERT INTO $TABLE_USERS ($COLUMN_EMAIL, $COLUMN_USERNAME, $COLUMN_PASSWORD) VALUES (?, ?, ?)"
            val stmt = db.compileStatement(sql)
            stmt.bindString(1, email)
            stmt.bindString(2, encryptedUsername)
            stmt.bindString(3, hashedPassword)
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
        val db = readableDatabase
        val encryptedUsername = encrypt(username)

        val cursor = db.rawQuery(
            "SELECT $COLUMN_PASSWORD, $COLUMN_FAILED_ATTEMPTS, $COLUMN_LAST_FAILED FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?",
            arrayOf(encryptedUsername)
        )

        if (cursor.moveToFirst()) {
            val hashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
            val failedAttempts = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FAILED_ATTEMPTS))
            val lastFailed = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_FAILED))
            cursor.close()

            // Check lockout
            val lockoutDuration = 5 * 60 * 1000 // 5 minutes
            if (failedAttempts >= 3 && System.currentTimeMillis() - lastFailed < lockoutDuration) {
                return false
            }

            if (BCrypt.checkpw(password, hashedPassword)) {
                resetFailedAttempts(username)
                return true
            } else {
                incrementFailedAttempts(username)
                return false
            }
        }
        cursor.close()
        return false
    }

    private fun incrementFailedAttempts(username: String) {
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

    private fun encrypt(data: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY)
        return Base64.encodeToString(cipher.doFinal(data.toByteArray()), Base64.DEFAULT)
    }

    private fun decrypt(data: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY)
        return String(cipher.doFinal(Base64.decode(data, Base64.DEFAULT)))
    }

    data class UserDetails(
        val email: String,
        val username: String
    )

    fun getUserDetails(username: String): UserDetails? {
        Log.d("DatabaseHelper", "Fetching details for username: $username")
        val db = readableDatabase

        // Encrypt the username before querying
        val encryptedUsername = encrypt(username)

        val cursor = db.rawQuery(
            "SELECT $COLUMN_EMAIL, $COLUMN_USERNAME FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?",
            arrayOf(encryptedUsername)  // Pass the encrypted username
        )

        return if (cursor.moveToFirst()) {
            val email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
            val fetchedUsername = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
            cursor.close()
            Log.d("DatabaseHelper", "Retrieved user details: $email, $fetchedUsername")
            val decryptedUsername = decrypt(fetchedUsername)
            UserDetails(email, decryptedUsername)
        } else {
            Log.d("DatabaseHelper", "No user found for username: $username")
            null
        }
    }


    fun updatePassword(username: String, newPassword: String): Boolean {
        val db = writableDatabase
        val encryptedUsername = encrypt(username)
        val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())

        return try {
            val sql = "UPDATE $TABLE_USERS SET $COLUMN_PASSWORD = ? WHERE $COLUMN_USERNAME = ?"
            val stmt = db.compileStatement(sql)
            stmt.bindString(1, hashedPassword)
            stmt.bindString(2, encryptedUsername)
            stmt.executeUpdateDelete() > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error updating password: ${e.message}")
            false
        } finally {
            db.close()
        }
    }



    fun isAdmin(username: String, password: String): Boolean {
        return username == "Admin" && password == "Admin_4002!"
    }

    fun unlockUser(username: String): Boolean {
        val db = writableDatabase
        return try {
            db.execSQL(
                "UPDATE $TABLE_USERS SET $COLUMN_FAILED_ATTEMPTS = 0 WHERE $COLUMN_USERNAME = ?",
                arrayOf(encrypt(username))
            )
            Log.d("DatabaseHelper", "User $username unlocked successfully.")
            true
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error unlocking user: ${e.message}")
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
            Log.d("DatabaseHelper", "User $username deleted successfully.")
            true
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error deleting user: ${e.message}")
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
}
