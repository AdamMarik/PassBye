package com.project.passbye.autofill

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.project.passbye.R
import java.util.concurrent.Executor

class AuthFillActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val username = intent.getStringExtra("username") ?: return finish()
        val password = intent.getStringExtra("password") ?: return finish()
        val usernameId = intent.getParcelableExtra<AutofillId>("usernameId") ?: return finish()
        val passwordId = intent.getParcelableExtra<AutofillId>("passwordId") ?: return finish()

        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val presentation = RemoteViews(packageName, R.layout.autofill_item).apply {
                    setTextViewText(R.id.username, username)
                    setTextViewText(R.id.domain, "Unlocked")
                }

                val dataset = Dataset.Builder(presentation)
                    .setValue(usernameId, AutofillValue.forText(username), presentation)
                    .setValue(passwordId, AutofillValue.forText(password), presentation)
                    .build()

                val replyIntent = Intent()
                replyIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
                setResult(Activity.RESULT_OK, replyIntent)
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(this@AuthFillActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to Autofill")
            .setSubtitle("Confirm fingerprint or PIN")
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
