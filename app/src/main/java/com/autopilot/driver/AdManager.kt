package com.autopilot.driver

import android.app.Activity
import android.app.Application
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK

object AdManager {
    private const val MIN_AD_INTERVAL_MS = 90_000L

    fun init(app: Application) {
        StartAppSDK.init(app, BuildConfig.STARTAPP_ID, true)
        StartAppSDK.setTestAdsEnabled(false)
    }

    fun showInterstitialIfAllowed(activity: Activity) {
        if (!AppPrefs.isAdConsentGranted) return
        if (System.currentTimeMillis() - AppPrefs.lastAdShownAt < MIN_AD_INTERVAL_MS) return
        AppPrefs.lastAdShownAt = System.currentTimeMillis()
        StartAppAd.showAd(activity)
    }
}
