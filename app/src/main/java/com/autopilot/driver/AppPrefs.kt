package com.autopilot.driver

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

object AppPrefs {
    const val AUTHORIZED_ADMIN_EMAIL = "aalamdiwan555@gmail.com"
    private lateinit var prefs: SharedPreferences
    private const val NAME = "autopilot_prefs"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    var authToken: String?
        get() = prefs.getString("auth_token", null)
        set(value) = prefs.edit().putString("auth_token", value).apply()

    var userEmail: String?
        get() = prefs.getString("user_email", null)
        set(value) = prefs.edit().putString("user_email", value).apply()

    var isOnboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) = prefs.edit().putBoolean("onboarding_complete", value).apply()

    var minPrice: Double
        get() = prefs.getString("min_price", "0")?.toDoubleOrNull() ?: 0.0
        set(value) = prefs.edit().putString("min_price", value.toString()).apply()

    var maxPrice: Double
        get() = prefs.getString("max_price", "99999")?.toDoubleOrNull() ?: 99999.0
        set(value) = prefs.edit().putString("max_price", value.toString()).apply()

    var isBotRunning: Boolean
        get() = prefs.getBoolean("bot_running", false)
        set(value) = prefs.edit().putBoolean("bot_running", value).apply()

    var subscriptionExpiry: Long
        get() = prefs.getLong("subscription_expiry", 0)
        set(value) = prefs.edit().putLong("subscription_expiry", value).apply()

    var rewardPoints: Int
        get() = prefs.getInt("reward_points", 0)
        set(value) = prefs.edit().putInt("reward_points", value.coerceAtLeast(0)).apply()

    var lastDailyRewardAt: Long
        get() = prefs.getLong("last_daily_reward_at", 0)
        set(value) = prefs.edit().putLong("last_daily_reward_at", value).apply()

    fun isAuthorizedAdmin(): Boolean {
        return isLoggedIn && userEmail?.trim()?.equals(AUTHORIZED_ADMIN_EMAIL, ignoreCase = true) == true
    }

    fun addRewardPoints(points: Int) {
        if (points > 0) rewardPoints = rewardPoints + points
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
        prefs.edit().clear().apply()
    }
}
