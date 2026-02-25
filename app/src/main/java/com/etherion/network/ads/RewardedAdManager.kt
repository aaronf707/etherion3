package com.etherion.network.ads

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.etherion.network.miner.MiningPersistence
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(
    private val context: Context,
    private val adUnitId: String
) {
    private var rewardedAd: RewardedAd? = null
    private var loading = false
    private val persistence = MiningPersistence(context)
    private val consentManager = if (context is Activity) ConsentManager(context) else null
    
    private var watchTimestamps = mutableListOf<Long>()
    private val MAX_ADS_PER_HOUR = 5
    private val HOUR_IN_MS = 60 * 60 * 1000L

    init {
        watchTimestamps = persistence.loadAdTimestamps().toMutableList()
    }

    fun isAvailable(): Boolean = rewardedAd != null && canWatchAd()

    private fun canWatchAd(): Boolean {
        cleanupTimestamps()
        return watchTimestamps.size < MAX_ADS_PER_HOUR
    }

    private fun cleanupTimestamps() {
        val now = System.currentTimeMillis()
        val originalSize = watchTimestamps.size
        watchTimestamps.removeAll { now - it > HOUR_IN_MS }
        if (watchTimestamps.size != originalSize) {
            persistence.saveAdTimestamps(watchTimestamps)
        }
    }

    fun getTimeUntilNextAd(): Long {
        cleanupTimestamps()
        if (watchTimestamps.isEmpty()) return 0
        val oldest = watchTimestamps.minOrNull() ?: return 0
        return maxOf(0, HOUR_IN_MS - (System.currentTimeMillis() - oldest))
    }

    fun load(onLoaded: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        // Check for GDPR consent before loading ads
        if (consentManager != null && !consentManager.canRequestAds()) {
            onFailure("Consent required")
            return
        }

        if (loading || rewardedAd != null) {
            if (rewardedAd != null) onLoaded()
            return
        }
        loading = true
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    loading = false
                    onLoaded()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    onFailure(error.message)
                }
            }
        )
    }

    fun show(activity: Activity, onReward: () -> Unit) {
        if (!canWatchAd()) {
            val minutesLeft = (getTimeUntilNextAd() / 60000) + 1
            Toast.makeText(context, "Ad limit reached. Please wait $minutesLeft minutes.", Toast.LENGTH_SHORT).show()
            return
        }

        val ad = rewardedAd ?: run {
            Toast.makeText(context, "Ad not ready. Try again in a moment.", Toast.LENGTH_SHORT).show()
            load() // Start loading for the next try
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                Toast.makeText(context, "Ad failed to show. Try again.", Toast.LENGTH_SHORT).show()
                load()
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                load()
            }
        }

        ad.show(activity) {
            watchTimestamps.add(System.currentTimeMillis())
            persistence.saveAdTimestamps(watchTimestamps)
            onReward()
        }
    }
}
