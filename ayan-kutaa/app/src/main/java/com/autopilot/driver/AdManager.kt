package com.autopilot.driver

import android.app.Activity
import android.app.Application
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import java.util.concurrent.TimeUnit

object AdManager {
    private const val MIN_INTERVAL_MS = 5 * 60 * 1000L
    private const val MAX_PER_DAY = 3
    private var initialized = false

    fun init(app: Application) {
        if (initialized) return
        StartAppSDK.init(app, BuildConfig.STARTAPP_ID, true)
        StartAppSDK.setTestAdsEnabled(false)
        initialized = true
    }

    /**
     * Ads are shown only after a deliberate user action, with consent,
     * a five-minute cooldown, and a three-impression daily cap.
     */
    fun showAfterUserAction(activity: Activity) {
        if (!initialized || !AppPrefs.isAdConsentGranted || activity.isFinishing) return
        val now = System.currentTimeMillis()
        if (now - AppPrefs.lastInterstitialAt < MIN_INTERVAL_MS) return

        val today = TimeUnit.MILLISECONDS.toDays(now)
        if (AppPrefs.interstitialDay != today) {
            AppPrefs.interstitialDay = today
            AppPrefs.interstitialsToday = 0
        }
        if (AppPrefs.interstitialsToday >= MAX_PER_DAY) return

        runCatching {
            StartAppAd.showAd(activity)
            AppPrefs.lastInterstitialAt = now
            AppPrefs.interstitialsToday += 1
        }
    }
}
