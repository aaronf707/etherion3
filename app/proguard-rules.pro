# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/youhu/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any custom keep rules here:

# Firebase specific rules
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }

# Google Play Services (Ads)
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }

# Keep your model classes if they are used for serialization (e.g. Firebase)
-keep class com.etherion.network.miner.MiningViewModel$MiningState { *; }
-keep class com.etherion.network.wallet.WalletViewModel$Transaction { *; }
-keep class com.etherion.network.domain.** { *; }

# Fix for Missing classes detected while running R8 (Android 14 / API 34 specific classes)
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
