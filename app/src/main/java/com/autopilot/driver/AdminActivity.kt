package com.autopilot.driver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.autopilot.driver.databinding.ActivityAdminBinding
import java.util.concurrent.TimeUnit
import kotlin.math.max

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!AppPrefs.isAuthorizedAdmin()) {
            Toast.makeText(this, "Admin access denied", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnAddSubscription.setOnClickListener {
            val days = binding.etDays.text.toString().toIntOrNull() ?: 0
            if (days <= 0) {
                Toast.makeText(this, "Enter valid days", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val expiry = max(AppPrefs.subscriptionExpiry, System.currentTimeMillis()) + TimeUnit.DAYS.toMillis(days.toLong())
            AppPrefs.subscriptionExpiry = expiry
            Toast.makeText(this, "Subscription added for $days days", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnResetSubscription.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset subscription?")
                .setMessage("This will remove all active subscription time.")
                .setPositiveButton("Reset") { _, _ ->
                    AppPrefs.subscriptionExpiry = 0
                    Toast.makeText(this, "Subscription reset", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AppPrefs.isAuthorizedAdmin()) {
            Toast.makeText(this, "Admin access revoked", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
