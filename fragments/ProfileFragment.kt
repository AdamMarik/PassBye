package com.project.passbye.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.project.passbye.R
import com.project.passbye.activities.ChangePasswordActivity
import com.project.passbye.activities.LoginActivity
import com.project.passbye.database.DatabaseHelper
import java.util.concurrent.Executor

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var passwordTextView: TextView
    private lateinit var passwordEye: ImageButton
    private var isPasswordVisible = false
    private var realPassword: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val databaseHelper = DatabaseHelper(requireContext())
        val sharedPreferences = requireContext().getSharedPreferences("PassByePrefs", 0)
        val loggedInUsername = sharedPreferences.getString("currentUser", null)

        val emailTextView: TextView = view.findViewById(R.id.emailTextView)
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        passwordTextView = view.findViewById(R.id.passwordTextView)
        passwordEye = view.findViewById(R.id.passwordEye)
        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        val changePasswordButton: Button = view.findViewById(R.id.changePasswordButton)

        if (loggedInUsername == null) {
            Toast.makeText(requireContext(), "No logged-in user found.", Toast.LENGTH_SHORT).show()
            return
        }

        val userDetails = databaseHelper.getUserDetails(loggedInUsername)
        if (userDetails != null) {
            emailTextView.text = "Email: ${userDetails.email}"
            usernameTextView.text = "Username: ${userDetails.username}"
            realPassword = userDetails.decryptedPassword
            passwordTextView.text = "Password: ********"
        } else {
            Toast.makeText(requireContext(), "Failed to load user details.", Toast.LENGTH_SHORT).show()
        }

        passwordEye.setOnClickListener {
            authenticateAndTogglePassword()
        }

        logoutButton.setOnClickListener {
            sharedPreferences.edit().remove("currentUser").apply()
            val intent = Intent(context, LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        changePasswordButton.setOnClickListener {
            val intent = Intent(context, ChangePasswordActivity::class.java)
            intent.putExtra("username", loggedInUsername)
            startActivity(intent)
        }
    }

    private fun authenticateAndTogglePassword() {
        val executor: Executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isPasswordVisible = !isPasswordVisible
                    passwordTextView.text = if (isPasswordVisible) "Password: $realPassword" else "Password: ********"
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(requireContext(), "Authentication required to view password", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify Identity")
            .setSubtitle("Biometric or device password required")
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
