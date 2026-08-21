package com.autopilot.driver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.autopilot.driver.databinding.ActivitySignupBinding
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.length !in 10..15 || !phone.all { it.isDigit() }) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val api = ApiClient.authApi()
            if (api == null) {
                Toast.makeText(this, "Authentication service is unavailable.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            binding.btnSignup.isEnabled = false
            lifecycleScope.launch {
                try {
                    val response = api.signup(SignupRequest(name, email, phone, password))
                    val body = response.body()
                    val responseEmail = body?.email
                    if (response.isSuccessful && body?.token?.isNotBlank() == true && !responseEmail.isNullOrBlank()) {
                        AppPrefs.setLoginSession(body.token, responseEmail, body.isAdmin)
                        startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@SignupActivity, "Unable to create the account.", Toast.LENGTH_LONG).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(this@SignupActivity, "Unable to reach the authentication service.", Toast.LENGTH_LONG).show()
                } finally {
                    binding.btnSignup.isEnabled = true
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
}
