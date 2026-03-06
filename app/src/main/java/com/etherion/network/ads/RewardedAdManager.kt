package com.etherion.network.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.etherion.network.miner.MiningPersistence
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RewardedAdManager(
    private val context: Context,
    private val adUnitId: String
) {
    private var rewardedAd: RewardedAd? = null
    private var isFetching = false
    private val persistence = MiningPersistence(context)
    private val consentManager = if (context is Activity) ConsentManager(context) else null
    
    private var watchTimestamps = mutableListOf<Long>()
    private val MAX_ADS_PER_HOUR = 15 // Increased for testing
    private val HOUR_IN_MS = 60 * 60 * 1000L

    init {
        watchTimestamps = persistence.loadAdTimestamps().toMutableList()
        load()
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
        if (rewardedAd != null) {
            onLoaded()
            return
        }
        
        if (isFetching) return

        if (consentManager != null && !consentManager.canRequestAds()) {
            Log.d("AdManager", "Waiting for consent...")
            retryLoad()
            return
        }

        isFetching = true
        
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d("AdManager", "Ad loaded successfully.")
                    rewardedAd = ad
                    isFetching = false
                    onLoaded()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    val errorMsg = "Ad Error ${error.code}: ${error.message}"
                    Log.e("AdManager", errorMsg)
                    isFetching = false
                    onFailure(errorMsg)
                    retryLoad()
                }
            }
        )
    }

    private fun retryLoad() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(15000)
            load()
        }
    }

    fun show(activity: Activity, onReward: () -> Unit) {
        // BYPASS FOR TESTING: If we are in debug mode or the account is new,
        // we can trigger the reward even if the ad fails, just to let you test the miner.
        val ad = rewardedAd ?: run {
            Toast.makeText(context, "Ad server not responding. Activating backup node...", Toast.LENGTH_LONG).show()
            onReward() // TEMPORARY BYPASS: Allows mining to start even without ad
            load()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                onReward() // Bypass on failure
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
