package com.autopilot.driver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autopilot.driver.databinding.ActivityAdminBinding
import java.util.concurrent.TimeUnit

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddSubscription.setOnClickListener {
            val days = binding.etDays.text.toString().toIntOrNull() ?: 0
            if (days <= 0) {
                Toast.makeText(this, "Enter valid days", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val expiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
            AppPrefs.subscriptionExpiry = expiry
            Toast.makeText(this, "Subscription added for $days days", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnResetSubscription.setOnClickListener {
            AppPrefs.subscriptionExpiry = 0
            Toast.makeText(this, "Subscription reset", Toast.LENGTH_SHORT).show()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
