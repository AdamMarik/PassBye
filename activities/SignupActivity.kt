package com.project.passbye.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.project.passbye.R
import com.project.passbye.database.DatabaseHelper

class SignupActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var signupButton: Button
    private lateinit var nameField: EditText
    private lateinit var emailField: EditText
    private lateinit var usernameField: EditText
    private lateinit var passwordField: EditText
    private lateinit var signupStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        dbHelper = DatabaseHelper(this)

        signupButton = findViewById(R.id.signupButton)
        nameField = findViewById(R.id.nameField)
        emailField = findViewById(R.id.emailField)
        usernameField = findViewById(R.id.usernameField)
        passwordField = findViewById(R.id.passwordField)
        signupStatusText = findViewById(R.id.signupStatusText)

        signupButton.setOnClickListener {
            val name = nameField.text.toString()
            val email = emailField.text.toString()
            val username = usernameField.text.toString()
            val password = passwordField.text.toString()

            if (name.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                signupStatusText.text = "All fields are required"
                signupStatusText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                signupStatusText.text = "Invalid email format"
                signupStatusText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!password.matches(Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$"))) {
                signupStatusText.text = "Password must be at least 8 characters, include letters and numbers."
                signupStatusText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (dbHelper.checkEmailExists(email)) {
                signupStatusText.text = "Email already exists"
                signupStatusText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val success = dbHelper.insertUser(email, username, password)
            if (success) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                signupStatusText.text = "Signup failed. Try again."
                signupStatusText.visibility = View.VISIBLE
            }
        }
    }
}
