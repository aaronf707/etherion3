package com.etherion.network.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val activity: Activity) {
    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)

    interface OnConsentCheckCompletedListener {
        fun onConsentCheckCompleted()
    }

    fun gatherConsent(onConsentCheckCompletedListener: OnConsentCheckCompletedListener) {
        // For testing, you can reset consent:
        // consentInformation.reset()

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // Consent has been gathered or isn't required
                    onConsentCheckCompletedListener.onConsentCheckCompleted()
                }
            },
            { requestConsentError ->
                // Consent info update failed
                onConsentCheckCompletedListener.onConsentCheckCompleted()
            }
        )
    }

    fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }
}
