package com.project.passbye.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.passbye.R
import com.project.passbye.database.DatabaseHelper

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val databaseHelper = DatabaseHelper(this)
        val username = intent.getStringExtra("username")

        val oldPasswordEditText: EditText = findViewById(R.id.oldPasswordEditText)
        val newPasswordEditText: EditText = findViewById(R.id.newPasswordEditText)
        val confirmPasswordEditText: EditText = findViewById(R.id.confirmPasswordEditText)
        val updatePasswordButton: Button = findViewById(R.id.updatePasswordButton)

        updatePasswordButton.setOnClickListener {
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Check if new password and confirm password match
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the new password is strong
            if (!isPasswordStrong(newPassword)) {
                Toast.makeText(this, "New password is not strong enough!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verify if the old password is correct and update the password
            if (username != null && databaseHelper.verifyUser(username, oldPassword)) {
                if (databaseHelper.updatePassword(username, newPassword)) {
                    Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update password.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Incorrect old password!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to check password strength
    private fun isPasswordStrong(password: String): Boolean {
        // Password must be at least 8 characters long and contain at least one letter and one number
        val regex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$".toRegex()
        return password.matches(regex)
    }
}