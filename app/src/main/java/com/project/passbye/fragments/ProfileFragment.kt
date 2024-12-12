package com.project.passbye.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.passbye.R
import com.project.passbye.activities.ChangePasswordActivity
import com.project.passbye.activities.LoginActivity
import com.project.passbye.database.DatabaseHelper

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Log fragment lifecycle
        Log.d("ProfileFragment", "onViewCreated called")

        // Initialize database helper
        val databaseHelper = DatabaseHelper(requireContext())

        // Retrieve current username from SharedPreferences or session
        val sharedPreferences = requireContext().getSharedPreferences("PassByePrefs", 0)
        val loggedInUsername = sharedPreferences.getString("currentUser", null)

        if (loggedInUsername.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No logged-in user found.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("ProfileFragment", "Logged-in username: $loggedInUsername")

        // Retrieve user details from the database
        val userDetails = databaseHelper.getUserDetails(loggedInUsername)
        Log.d("ProfileFragment", "UserDetails fetched: $userDetails")

        // Initialize views
        val emailTextView: TextView = view.findViewById(R.id.emailTextView)
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val passwordTextView: TextView = view.findViewById(R.id.passwordTextView)
        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        val changePasswordButton: Button = view.findViewById(R.id.changePasswordButton)


        if (userDetails != null) {
            emailTextView.text = "Email: ${userDetails.email}"
            usernameTextView.text  = "Username: ${userDetails.username}"
            passwordTextView.text = "Password: ********"
        } else {
            Toast.makeText(requireContext(), "Failed to load user details.", Toast.LENGTH_SHORT).show()
        }


        // Set logout button functionality
        logoutButton.setOnClickListener {
            Log.d("ProfileFragment", "Logout button clicked")
            sharedPreferences.edit().remove("currentUser").apply()
            val intent = Intent(context, LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        // Set change password button functionality
        changePasswordButton.setOnClickListener {
            Log.d("ProfileFragment", "Change Password button clicked")
            val intent = Intent(context, ChangePasswordActivity::class.java)
            intent.putExtra("username", loggedInUsername) // Pass username to ChangePasswordActivity
            startActivity(intent)
        }
    }
}
