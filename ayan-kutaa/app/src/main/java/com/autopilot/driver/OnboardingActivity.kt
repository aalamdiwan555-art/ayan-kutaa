package com.autopilot.driver

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autopilot.driver.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (AppPrefs.isOnboardingComplete) {
            if (AppPrefs.isLoggedIn) {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } else {
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            finish()
            return
        }

        binding.btnGrantAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnGrantOverlay.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }

        binding.btnContinue.setOnClickListener {
            AppPrefs.isOnboardingComplete = true
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val accessibilityEnabled = isAccessibilityEnabled()
        val overlayEnabled = Settings.canDrawOverlays(this)

        binding.tvAccessibilityStatus.text = if (accessibilityEnabled) "✓ Accessibility Enabled" else "✗ Accessibility Required"
        binding.tvAccessibilityStatus.setTextColor(
            ContextCompat.getColor(this, if (accessibilityEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )

        binding.tvOverlayStatus.text = if (overlayEnabled) "✓ Overlay Enabled" else "✗ Overlay Required"
        binding.tvOverlayStatus.setTextColor(
            ContextCompat.getColor(this, if (overlayEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )

        binding.btnContinue.isEnabled = accessibilityEnabled && overlayEnabled
        if (AppPrefs.isOnboardingComplete && accessibilityEnabled && overlayEnabled) {
            navigateToLogin()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val expected = "$packageName/.RideAccessibilityService"
        return enabledServices.split(":").any { it.trim() == expected }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }
}
