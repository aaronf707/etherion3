package com.etherion.network.ui.ads

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.etherion.network.ads.ConsentManager
import com.etherion.network.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val consentManager = remember(activity) { activity?.let { ConsentManager(it) } }
    
    val canShowAds = consentManager?.canRequestAds() ?: true
    var isAdVisible by remember { mutableStateOf(false) }

    if (canShowAds) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(if (isAdVisible) 50.dp else 0.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = ctx.getString(R.string.admob_banner_id)
                    adListener = object : com.google.android.gms.ads.AdListener() {
                        override fun onAdLoaded() {
                            isAdVisible = true
                        }
                        override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                            isAdVisible = false
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

private fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
