package com.project.passbye.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.passbye.R
import com.project.passbye.database.DatabaseHelper

class SignupActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        databaseHelper = DatabaseHelper(this)

        val emailField = findViewById<EditText>(R.id.emailField)
        val usernameField = findViewById<EditText>(R.id.usernameField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val signupButton = findViewById<Button>(R.id.signupButton)

        signupButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val username = usernameField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
            } else if (!isValidEmail(email)) {
                Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show()
            } else if (!isPasswordStrong(password)) {
                Toast.makeText(this, "Password must be at least 8 characters, include letters and numbers.", Toast.LENGTH_SHORT).show()
            } else if (databaseHelper.insertUser(email, username, password)) {
                Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Signup failed. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordStrong(password: String): Boolean {
        val regex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$".toRegex()
        return password.matches(regex)
    }
}
