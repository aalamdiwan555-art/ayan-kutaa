package com.autopilot.driver

import android.app.Activity
import android.app.Application
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK

object AdManager {
    fun init(app: Application) {
        StartAppSDK.init(app, "207133232", true)
        StartAppSDK.setTestAdsEnabled(false)
    }

    fun showInterstitial(activity: Activity) {
        StartAppAd.showAd(activity)
    }
}
