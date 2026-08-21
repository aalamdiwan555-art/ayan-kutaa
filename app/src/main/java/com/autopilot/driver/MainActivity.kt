package com.autopilot.driver

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autopilot.driver.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!AppPrefs.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.etMinPrice.setText(AppPrefs.minPrice.toString())
        binding.etMaxPrice.setText(AppPrefs.maxPrice.toString())

        binding.btnToggleBot.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Enable Accessibility first", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }

            if (BotState.isRunning) {
                stopBot()
            } else {
                val min = binding.etMinPrice.text.toString().toDoubleOrNull() ?: 0.0
                val max = binding.etMaxPrice.text.toString().toDoubleOrNull() ?: 99999.0
                if (max < min) {
                    Toast.makeText(this, "Max must be >= Min", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AppPrefs.minPrice = min
                AppPrefs.maxPrice = max
                startBot()
            }
        }

        updateSubscriptionUI()

        binding.btnAdmin.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            BotState.isRunning = false
            AppPrefs.isBotRunning = false
            AppPrefs.clear()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateBotUI()
        updateSubscriptionUI()
    }

    private fun startBot() {
        if (!AppPrefs.hasActiveSubscription()) {
            Toast.makeText(this, "Subscription expired. Contact admin.", Toast.LENGTH_LONG).show()
            return
        }
        BotState.isRunning = true
        AppPrefs.isBotRunning = true
        Toast.makeText(this, "Auto-Accepter STARTED", Toast.LENGTH_SHORT).show()
        updateBotUI()
    }

    private fun stopBot() {
        BotState.isRunning = false
        AppPrefs.isBotRunning = false
        Toast.makeText(this, "Auto-Accepter STOPPED", Toast.LENGTH_SHORT).show()
        updateBotUI()
    }

    private fun updateBotUI() {
        if (BotState.isRunning) {
            binding.tvBotStatus.text = "Status: RUNNING"
            binding.tvBotStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            binding.btnToggleBot.text = "STOP"
            binding.btnToggleBot.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            binding.tvBotStatus.text = "Status: STOPPED"
            binding.tvBotStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.btnToggleBot.text = "START"
            binding.btnToggleBot.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        }
    }

    private fun updateSubscriptionUI() {
        val hasSub = AppPrefs.hasActiveSubscription()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val expiry = sdf.format(Date(AppPrefs.subscriptionExpiry))
        binding.tvSubscription.text = if (hasSub) "Active until $expiry" else "No active subscription"
        binding.tvSubscription.setTextColor(
            ContextCompat.getColor(this, if (hasSub) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabledServices.contains(packageName)
    }
}
