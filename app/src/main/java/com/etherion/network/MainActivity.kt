package com.etherion.network

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.etherion.network.ads.ConsentManager
import com.etherion.network.ui.navigation.AppNavigation
import com.etherion.network.ui.theme.EtherionTheme
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

class MainActivity : ComponentActivity() {

    private lateinit var consentManager: ConsentManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition before super.onCreate
        installSplashScreen()

        // Force no title bar before super.onCreate
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase App Check
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        requestNotificationPermission()
        
        // Handle GDPR / EU Consent
        consentManager = ConsentManager(this)
        consentManager.gatherConsent(object : ConsentManager.OnConsentCheckCompletedListener {
            override fun onConsentCheckCompleted() {
                Log.d("Etherion", "Consent check completed. Ads allowed: ${consentManager.canRequestAds()}")
            }
        })

        setContent {
            EtherionTheme {
                AppNavigation()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }
}
