package com.autopilot.driver

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

object AppPrefs {
    private lateinit var prefs: SharedPreferences
    private const val NAME = "autopilot_secure_prefs"

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Encrypted preferences unavailable; using protected fallback", exception)
            context.getSharedPreferences("${NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) {
            prefs.edit().putBoolean("is_logged_in", value).commit()
        }

    var authToken: String?
        get() = prefs.getString("auth_token", null)
        set(value) {
            prefs.edit().putString("auth_token", value).commit()
        }

    var userEmail: String?
        get() = prefs.getString("user_email", null)
        set(value) {
            prefs.edit().putString("user_email", value).commit()
        }

    var isAdmin: Boolean
        get() = prefs.getBoolean("is_admin", false)
        set(value) {
            prefs.edit().putBoolean("is_admin", value).apply()
        }

    fun setLoginSession(token: String, email: String, admin: Boolean) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("auth_token", token)
            .putString("user_email", email)
            .putBoolean("is_admin", admin)
            .commit()
    }

    var isOnboardingComplete: Boolean
        get() = prefs.getBoolean("onboarding_complete", false)
        set(value) {
            prefs.edit().putBoolean("onboarding_complete", value).commit()
        }

    var minPrice: Double
        get() = prefs.getLong("min_price_cents", 0L) / 100.0
        set(value) {
            prefs.edit().putLong("min_price_cents", (value.coerceAtLeast(0.0) * 100).roundToLong()).apply()
        }

    var maxPrice: Double
        get() = prefs.getLong("max_price_cents", 9_999_900L) / 100.0
        set(value) {
            prefs.edit().putLong("max_price_cents", (value.coerceAtLeast(0.0) * 100).roundToLong()).apply()
        }

    var isBotRunning: Boolean
        get() = prefs.getBoolean("bot_running", false)
        set(value) {
            prefs.edit().putBoolean("bot_running", value).commit()
        }

    var subscriptionExpiry: Long
        get() = prefs.getLong("subscription_expiry", 0)
        set(value) {
            prefs.edit().putLong("subscription_expiry", value).commit()
        }

    var rewardPoints: Int
        get() = prefs.getInt("reward_points", 0)
        set(value) {
            prefs.edit().putInt("reward_points", value.coerceAtLeast(0)).commit()
        }

    var lastDailyRewardAt: Long
        get() = prefs.getLong("last_daily_reward_at", 0)
        set(value) {
            prefs.edit().putLong("last_daily_reward_at", value).commit()
        }

    var isAdConsentGranted: Boolean
        get() = prefs.getBoolean("ad_consent_granted", false)
        set(value) {
            prefs.edit().putBoolean("ad_consent_granted", value).commit()
        }

    var lastInterstitialAt: Long
        get() = prefs.getLong("last_interstitial_at", 0L)
        set(value) {
            prefs.edit().putLong("last_interstitial_at", value).apply()
        }

    var interstitialDay: Long
        get() = prefs.getLong("interstitial_day", 0L)
        set(value) {
            prefs.edit().putLong("interstitial_day", value).apply()
        }

    var interstitialsToday: Int
        get() = prefs.getInt("interstitials_today", 0)
        set(value) {
            prefs.edit().putInt("interstitials_today", value.coerceAtLeast(0)).apply()
        }

    var rewardedAdsWatched: Int
        get() = prefs.getInt("rewarded_ads_watched", 0)
        set(value) {
            prefs.edit().putInt("rewarded_ads_watched", value.coerceIn(0, REWARDED_ADS_REQUIRED - 1)).apply()
        }

    fun recordRewardedAdCompletion(): Boolean {
        val nextCount = rewardedAdsWatched + 1
        return if (nextCount >= REWARDED_ADS_REQUIRED) {
            val now = System.currentTimeMillis()
            prefs.edit()
                .putInt("rewarded_ads_watched", 0)
                .putLong("subscription_expiry", maxOf(subscriptionExpiry, now) + TimeUnit.DAYS.toMillis(1))
                .commit()
            true
        } else {
            prefs.edit().putInt("rewarded_ads_watched", nextCount).commit()
            false
        }
    }

    fun isAuthorizedAdmin(): Boolean {
        // The admin claim must come from a trusted authentication response.
        // Never grant admin access based on a client-editable email address.
        return isLoggedIn && isAdmin
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

    private const val TAG = "AppPrefs"
    private const val REWARDED_ADS_REQUIRED = 10
}
