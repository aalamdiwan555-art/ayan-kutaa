package com.autopilot.driver

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.TimeUnit

object AppPrefs {
    const val AUTHORIZED_ADMIN_EMAIL = "aalamdiwan555@gmail.com"
    private lateinit var prefs: SharedPreferences
    private const val NAME = "autopilot_secure_prefs"

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).commit()

    var authToken: String?
        get() = prefs.getString("auth_token", null)
        set(value) = prefs.edit().putString("auth_token", value).commit()

    var userEmail: String?
        get() = prefs.getString("user_email", null)
        set(value) = prefs.edit().putString("user_email", value).commit()

    var isOnboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) = prefs.edit().putBoolean("onboarding_complete", value).commit()

    var minPrice: Double
        get() = prefs.getFloat("min_price", 0f).toDouble()
        set(value) = prefs.edit().putFloat("min_price", value.toFloat()).commit()

    var maxPrice: Double
        get() = prefs.getFloat("max_price", 99999f).toDouble()
        set(value) = prefs.edit().putFloat("max_price", value.toFloat()).commit()

    var isBotRunning: Boolean
        get() = prefs.getBoolean("bot_running", false)
        set(value) = prefs.edit().putBoolean("bot_running", value).commit()

    var subscriptionExpiry: Long
        get() = prefs.getLong("subscription_expiry", 0)
        set(value) = prefs.edit().putLong("subscription_expiry", value).commit()

    var rewardPoints: Int
        get() = prefs.getInt("reward_points", 0)
        set(value) = prefs.edit().putInt("reward_points", value.coerceAtLeast(0)).commit()

    var lastDailyRewardAt: Long
        get() = prefs.getLong("last_daily_reward_at", 0)
        set(value) = prefs.edit().putLong("last_daily_reward_at", value).commit()

    var isAdConsentGranted: Boolean
        get() = prefs.getBoolean("ad_consent_granted", false)
        set(value) = prefs.edit().putBoolean("ad_consent_granted", value).commit()

    fun isAuthorizedAdmin(): Boolean {
        return isLoggedIn && userEmail?.trim()?.equals(AUTHORIZED_ADMIN_EMAIL, ignoreCase = true) == true
    }

    fun addRewardPoints(points: Int) {
        if (points > 0) prefs.edit().run {
            val current = prefs.getInt("reward_points", 0)
            putInt("reward_points", (current + points).coerceAtLeast(0))
            commit()
        }
    }

    fun claimDailyReward(): Boolean {
        val now = System.currentTimeMillis()
        val day = TimeUnit.DAYS.toMillis(1)
        if (now - lastDailyRewardAt < day) return false
        addRewardPoints(25)
        lastDailyRewardAt = now
        return true
    }

    fun redeemRewardForSubscription(): Boolean {
        if (rewardPoints < 100) return false
        rewardPoints -= 100
        val now = System.currentTimeMillis()
        subscriptionExpiry = maxOf(subscriptionExpiry, now) + TimeUnit.DAYS.toMillis(1)
        return true
    }

    fun hasActiveSubscription(): Boolean {
        return System.currentTimeMillis() < subscriptionExpiry
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    fun isInitialized(): Boolean = ::prefs.isInitialized
}
