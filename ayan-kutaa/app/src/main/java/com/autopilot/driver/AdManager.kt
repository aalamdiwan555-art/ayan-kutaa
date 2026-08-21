package com.autopilot.driver

import android.app.Activity
import android.app.Application
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsInitializationError
import com.unity3d.ads.UnityAdsShowCompletionState
import com.unity3d.ads.UnityAdsShowError
import java.util.concurrent.TimeUnit

object AdManager {
    private const val MIN_INTERVAL_MS = 5 * 60 * 1000L
    private const val MAX_PER_DAY = 3
    private var initialized = false
    private var ready = false

    fun init(app: Application) {
        if (initialized) return
        initialized = true
        UnityAds.initialize(
            app,
            BuildConfig.UNITY_GAME_ID,
            false,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    ready = true
                }

                override fun onInitializationFailed(
                    error: UnityAdsInitializationError?,
                    message: String?
                ) {
                    ready = false
                }
            }
        )
    }

    /**
     * Ads are shown only after a deliberate user action, with consent,
     * a five-minute cooldown, and a three-impression daily cap.
     */
    fun showAfterUserAction(activity: Activity) {
        if (!initialized || !ready || !AppPrefs.isAdConsentGranted || activity.isFinishing) return
        val now = System.currentTimeMillis()
        if (now - AppPrefs.lastInterstitialAt < MIN_INTERVAL_MS) return

        val today = TimeUnit.MILLISECONDS.toDays(now)
        if (AppPrefs.interstitialDay != today) {
            AppPrefs.interstitialDay = today
            AppPrefs.interstitialsToday = 0
        }
        if (AppPrefs.interstitialsToday >= MAX_PER_DAY) return

        if (!UnityAds.isReady(BuildConfig.UNITY_INTERSTITIAL_AD_UNIT)) return
        UnityAds.show(activity, BuildConfig.UNITY_INTERSTITIAL_AD_UNIT, object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                placementId: String?,
                error: UnityAdsShowError?,
                message: String?
            ) = Unit

            override fun onUnityAdsShowStart(placementId: String?) {
                AppPrefs.lastInterstitialAt = now
                AppPrefs.interstitialsToday += 1
            }

            override fun onUnityAdsShowClick(placementId: String?) = Unit

            override fun onUnityAdsShowComplete(
                placementId: String?,
                state: UnityAdsShowCompletionState?
            ) = Unit
        }
    }
}
