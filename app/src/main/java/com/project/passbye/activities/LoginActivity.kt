package com.project.passbye.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.passbye.R
import com.project.passbye.database.DatabaseHelper

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

        // SharedPreferences to store current session
        val sharedPreferences = getSharedPreferences("PassByePrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        loginButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            when {
                username.isEmpty() -> {
                    Toast.makeText(this, "Please enter your username.", Toast.LENGTH_SHORT).show()
                }
                password.isEmpty() -> {
                    Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show()
                }
                databaseHelper.isAdmin(username, password) -> {
                    Toast.makeText(this, "Admin login successful!", Toast.LENGTH_SHORT).show()
                    editor.putString("currentUser", username)
                    editor.putBoolean("isAdmin", true)
                    editor.apply()
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                }
                databaseHelper.verifyUser(username, password) -> {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    editor.putString("currentUser", username)
                    editor.putBoolean("isAdmin", false)
                    editor.apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                else -> {
                    Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        signupButton.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
}
