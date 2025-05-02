package com.project.passbye.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.project.passbye.R
import java.security.SecureRandom

class PasswordGeneratorFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_password_generator, container, false)

        val passwordView = view.findViewById<TextView>(R.id.generated_password)
        val generateBtn = view.findViewById<Button>(R.id.generate_btn)
        val copyBtn = view.findViewById<Button>(R.id.copy_btn)

        fun generateAndShow() {
            val newPass = generateSecurePassword()
            passwordView.text = newPass
        }

        generateBtn.setOnClickListener {
            generateAndShow()
        }

        copyBtn.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("password", passwordView.text.toString()))
            Toast.makeText(requireContext(), "Password copied", Toast.LENGTH_SHORT).show()
        }

        generateAndShow()
        return view
    }

    private fun generateSecurePassword(length: Int = 20): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()_+-=[]{}|;:',.<>?/`~"
        val random = SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}
