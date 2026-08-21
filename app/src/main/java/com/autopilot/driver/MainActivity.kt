package com.autopilot.driver

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
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
        if (savedInstanceState == null) {
            BotState.isRunning = AppPrefs.isBotRunning && AppPrefs.hasActiveSubscription()
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
                if (!min.isFinite() || !max.isFinite() || min < 0 || max < 0) {
                    Toast.makeText(this, "Enter valid price values", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
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
        updateRewardsUI()
        binding.btnAdmin.visibility = if (AppPrefs.isAuthorizedAdmin()) View.VISIBLE else View.GONE

        binding.btnClaimReward.setOnClickListener {
            if (AppPrefs.claimDailyReward()) {
                Toast.makeText(this, "Daily reward claimed: +25 points", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Daily reward already claimed", Toast.LENGTH_SHORT).show()
            }
            updateRewardsUI()
        }

        binding.btnRedeemReward.setOnClickListener {
            if (AppPrefs.redeemRewardForSubscription()) {
                Toast.makeText(this, "Reward redeemed: 1 subscription day added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "You need 100 points to redeem", Toast.LENGTH_SHORT).show()
            }
            updateSubscriptionUI()
            updateRewardsUI()
        }

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
        updateRewardsUI()
        binding.btnAdmin.visibility = if (AppPrefs.isAuthorizedAdmin()) View.VISIBLE else View.GONE
    }

    private fun startBot() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission required", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            return
        }
        if (!AppPrefs.hasActiveSubscription()) {
            Toast.makeText(this, "Subscription expired. Contact admin.", Toast.LENGTH_LONG).show()
            return
        }
        if (!isAccessibilityServiceAvailable()) {
            Toast.makeText(this, "Accessibility service is unavailable. Please enable it again.", Toast.LENGTH_LONG).show()
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
            binding.btnToggleBot.setBackgroundResource(com.autopilot.driver.R.drawable.bg_button_secondary)
        } else {
            binding.tvBotStatus.text = "Status: STOPPED"
            binding.tvBotStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.btnToggleBot.text = "START"
            binding.btnToggleBot.setBackgroundResource(com.autopilot.driver.R.drawable.bg_button_primary)
        }
    }

    private fun updateSubscriptionUI() {
        val hasSub = AppPrefs.hasActiveSubscription()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val expiry = sdf.format(Date(AppPrefs.subscriptionExpiry))
        binding.tvSubscription.text = if (hasSub) "Active until $expiry" else "No active subscription"
        binding.tvSubscription.setTextColor(
            ContextCompat.getColor(this, if (hasSub) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
    }

    private fun updateRewardsUI() {
        binding.tvRewards.text = "Rewards: ${AppPrefs.rewardPoints} points"
        binding.btnRedeemReward.isEnabled = AppPrefs.rewardPoints >= 100
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = "$packageName/.RideAccessibilityService"
        return enabledServices.split(":").any { it.trim() == expected }
    }

    private fun isAccessibilityServiceAvailable(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            .any { it.resolveInfo.serviceInfo.packageName == packageName &&
                it.resolveInfo.serviceInfo.name == "$packageName.RideAccessibilityService" }
    }
}
