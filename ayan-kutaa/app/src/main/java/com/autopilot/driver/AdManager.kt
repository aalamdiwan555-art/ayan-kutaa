package com.autopilot.driver

import android.app.Activity
import android.app.Application
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK

object AdManager {
    fun init(app: Application) {
        StartAppSDK.init(app, BuildConfig.STARTAPP_ID, true)
        StartAppSDK.setTestAdsEnabled(false)
    }

    fun showInterstitial(activity: Activity) {
        if (AppPrefs.isAdConsentGranted) StartAppAd.showAd(activity)
    }
}
