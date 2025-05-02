package com.project.passbye.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.project.passbye.R
import com.project.passbye.database.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        databaseHelper = DatabaseHelper(this)

        val usernameField = findViewById<EditText>(R.id.usernameField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupButton = findViewById<Button>(R.id.signupButton)

        val sharedPreferences = getSharedPreferences("PassByePrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        loginButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showStatus("Please fill in all fields.", isSuccess = false)
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val isAdmin = databaseHelper.isAdmin(username, password)
                val isUser = databaseHelper.verifyUser(username, password)

                val failedAttemptsLocked = databaseHelper.getFailedAttempts(username) >= 5

                withContext(Dispatchers.Main) {
                    when {
                        isAdmin -> {
                            showStatus("Admin login successful!", isSuccess = true)
                            editor.putString("currentUser", username)
                            editor.putBoolean("isAdmin", true)
                            editor.apply()
                            startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
                            finish()
                        }

                        isUser -> {
                            showStatus("Login successful!", isSuccess = true)
                            editor.putString("currentUser", username)
                            editor.putBoolean("isAdmin", false)
                            editor.apply()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }

                        failedAttemptsLocked -> {
                            showStatus("Account locked after 5 failed attempts. Contact admin.", isSuccess = false)
                        }

                        else -> {
                            showStatus("Invalid credentials. Please try again.", isSuccess = false)
                        }
                    }
                }
            }
        }

        signupButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun showStatus(message: String, isSuccess: Boolean) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        val statusText = findViewById<TextView>(R.id.loginStatusText)
        statusText.setTextColor(
            getColor(
                if (isSuccess) android.R.color.holo_green_dark
                else android.R.color.holo_red_dark
            )
        )
        statusText.text = message
        statusText.visibility = View.VISIBLE
    }
}
