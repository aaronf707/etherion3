package com.etherion.network

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EtherionApplication : Application() {

    // Global scope for app-wide asynchronous tasks (SDK initializations)
    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        
        // Initialize AdMob SDK as early as possible
        applicationScope.launch {
            MobileAds.initialize(this@EtherionApplication)
        }
    }
}
