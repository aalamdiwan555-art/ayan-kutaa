package com.autopilot.driver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autopilot.driver.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.etEmail.setText(AppPrefs.userEmail.orEmpty())

        binding.btnReset.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val password = binding.etNewPassword.text.toString()
            if (password.length < 8) {
                Toast.makeText(this, "New password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (AppPrefs.resetPassword(email, password)) {
                AppPrefs.isLoggedIn = false
                Toast.makeText(this, "Password updated. You can log in now.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "No account found for this email", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvBack.setOnClickListener {
            finish()
        }
    }
}
