package com.autopilot.driver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.autopilot.driver.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            val api = ApiClient.authApi()
            if (api == null) {
                Toast.makeText(this, "Password reset service is unavailable.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            binding.btnReset.isEnabled = false
            lifecycleScope.launch {
                try {
                    val response = api.forgotPassword(EmailRequest(email))
                    if (response.isSuccessful) {
                        Toast.makeText(this@ForgotPasswordActivity, "Reset link sent if the account exists.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "Unable to send reset link.", Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(this@ForgotPasswordActivity, "Unable to reach the password reset service.", Toast.LENGTH_LONG).show()
                } finally {
                    binding.btnReset.isEnabled = true
                }
            }
        }

        binding.tvBack.setOnClickListener {
            finish()
        }
    }
}
