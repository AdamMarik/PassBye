package com.project.passbye.fragments

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import android.view.*
import android.widget.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.project.passbye.R
import com.project.passbye.util.EncryptedStorage
import kotlinx.coroutines.*

class PasswordsFragment : Fragment() {

    private lateinit var container: LinearLayout
    private lateinit var addButton: Button
    private lateinit var generatorButton: Button
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var sharedPreferences: SharedPreferences
    private var currentUser: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_passwords, container, false)

        sharedPreferences = requireContext().getSharedPreferences("PassByePrefs", Context.MODE_PRIVATE)
        currentUser = sharedPreferences.getString("currentUser", null)

        if (currentUser == null) {
            Toast.makeText(requireContext(), "No user logged in", Toast.LENGTH_SHORT).show()
            return view
        }

        this.container = view.findViewById(R.id.password_list_container)
        this.addButton = view.findViewById(R.id.add_credential_button)
        this.generatorButton = view.findViewById(R.id.open_password_generator)

        addButton.setOnClickListener {
            showAddCredentialDialog()
        }

        generatorButton.setOnClickListener {
            showPasswordGeneratorDialog()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        authenticateUser()
    }

    private fun authenticateUser() {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    loadPasswords()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptPasswordDialog()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Passwords")
            .setSubtitle("Authenticate to access")
            .setNegativeButtonText("Use app password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun promptPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter App Password")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val enteredPassword = input.text.toString()
            if (enteredPassword == "test1234") {
                loadPasswords()
            } else {
                Toast.makeText(requireContext(), "Incorrect password", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showAddCredentialDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add New Credential")

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 20, 30, 10)
        }

        val dnsInput = EditText(requireContext()).apply { hint = "Website DNS (e.g. https://example.com)" }
        val usernameInput = EditText(requireContext()).apply { hint = "Username or Email" }

        val passwordRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val passwordInput = EditText(requireContext()).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val strengthText = TextView(requireContext()).apply {
            text = "❓"
            setPadding(12, 0, 0, 0)
        }

        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val strength = calculatePasswordStrength(s.toString())
                strengthText.text = strength
                strengthText.setTextColor(
                    when (strength) {
                        "Weak" -> Color.RED
                        "Medium" -> Color.parseColor("#FFA500")
                        "Strong" -> Color.GREEN
                        else -> Color.GRAY
                    }
                )
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        passwordRow.addView(passwordInput)
        passwordRow.addView(strengthText)

        layout.addView(dnsInput)
        layout.addView(usernameInput)
        layout.addView(passwordRow)

        builder.setView(layout)

        builder.setPositiveButton("Save") { _, _ ->
            val dns = dnsInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            val isUrlValid = android.util.Patterns.WEB_URL.matcher(dns).matches()
            if (!isUrlValid) {
                Toast.makeText(requireContext(), "Please enter a valid website URL", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val cleanDomain = dns.removePrefix("https://").removePrefix("http://").removeSuffix("/").lowercase()

            val storage = EncryptedStorage(requireContext())
            storage.savePassword(currentUser!!, cleanDomain, username, password)

            loadPasswords()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun generateSecurePassword(length: Int = 20): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()_+-=[]{}|;:',.<>?/`~"
        val random = java.security.SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    private fun showPasswordGeneratorDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Generate Strong Password")

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 20)
        }

        val passwordView = TextView(requireContext()).apply {
            textSize = 18f
            setPadding(0, 0, 0, 20)
        }

        val generateBtn = Button(requireContext()).apply {
            text = "🔁 Generate New"
            setOnClickListener {
                passwordView.text = generateSecurePassword()
            }
        }

        val copyBtn = Button(requireContext()).apply {
            text = "📋 Copy to Clipboard"
            setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("password", passwordView.text.toString()))
                Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(passwordView)
        layout.addView(generateBtn)
        layout.addView(copyBtn)

        builder.setView(layout)
        builder.setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
        builder.show()

        passwordView.text = generateSecurePassword()
    }

    private fun calculatePasswordStrength(password: String): String {
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when (score) {
            in 0..2 -> "Weak"
            in 3..4 -> "Medium"
            else -> "Strong"
        }
    }

    private fun loadPasswords() {
        scope.launch {
            container.removeAllViews()
            val storage = EncryptedStorage(requireContext())
            val entries = storage.loadPasswordsForUser(currentUser!!)

            entries.forEach { (fullKey, password) ->
                val (_, domain, siteUsername) = fullKey.split("|")

                val credentialLayout = FrameLayout(requireContext())
                val content = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(30, 20, 30, 20)
                }

                val domainRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val domainLabel = TextView(requireContext()).apply {
                        text = "🌐 Domain: $domain"
                        textSize = 14f
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val openUrl = ImageButton(requireContext()).apply {
                        setImageResource(android.R.drawable.ic_menu_view)
                        background = null
                        setOnClickListener {
                            val fullUrl = if (domain.startsWith("http")) domain else "https://$domain"
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(fullUrl))
                            startActivity(intent)
                        }
                    }

                    addView(domainLabel)
                    addView(openUrl)
                }

                val emailRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val emailText = TextView(requireContext()).apply {
                        text = "👤 $siteUsername"
                        textSize = 16f
                    }
                    val copyEmail = ImageButton(requireContext()).apply {
                        setImageResource(R.drawable.ic_menu_copy)
                        background = null
                        setOnClickListener {
                            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("email", siteUsername))
                            Toast.makeText(requireContext(), "Copied email", Toast.LENGTH_SHORT).show()
                        }
                    }
                    addView(emailText)
                    addView(copyEmail)
                }

                val passwordRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val passwordText = TextView(requireContext()).apply {
                        text = "🔑 ${"•".repeat(password.length)}"
                        textSize = 16f
                        tag = password
                    }
                    val showToggle = ImageButton(requireContext()).apply {
                        setImageResource(android.R.drawable.ic_menu_view)
                        background = null
                        setOnClickListener {
                            val shown = passwordText.text.contains("•")
                            passwordText.text = if (shown) "🔑 $password" else "🔑 ${"•".repeat(password.length)}"
                        }
                    }
                    val copyPw = ImageButton(requireContext()).apply {
                        setImageResource(R.drawable.ic_menu_copy)
                        background = null
                        setOnClickListener {
                            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("password", password))
                            Toast.makeText(requireContext(), "Copied password", Toast.LENGTH_SHORT).show()
                        }
                    }
                    addView(passwordText)
                    addView(showToggle)
                    addView(copyPw)
                }

                val deleteBtn = ImageButton(requireContext()).apply {
                    setImageResource(android.R.drawable.ic_menu_delete)
                    background = null
                    setOnClickListener {
                        val storage = EncryptedStorage(requireContext())
                        storage.deletePassword(currentUser!!, domain, siteUsername)
                        loadPasswords()
                    }
                }

                val deleteParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END or Gravity.BOTTOM
                    marginEnd = 20
                    bottomMargin = 10
                }

                content.addView(domainRow)
                content.addView(emailRow)
                content.addView(passwordRow)

                credentialLayout.addView(content)
                credentialLayout.addView(deleteBtn, deleteParams)

                container.addView(credentialLayout)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
