package com.etherion.network.domain

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * BrightManager handles the integration of the Bright SDK.
 * This SDK allows users to earn boosts by sharing unused internet bandwidth.
 * 
 * BRIGHT SDK INTEGRATION GUIDE:
 * 1. Initialize the SDK in setEnabled(true).
 * 2. Monitor connectivity and throughput to adjust multipliers.
 * 3. Ensure the privacy policy is accepted before activation.
 */
class BrightManager(private val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore("bright_store")
        private val KEY_BRIGHT_ENABLED = booleanPreferencesKey("bright_enabled")
        private val KEY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
    }

    val isEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_BRIGHT_ENABLED] ?: false }
    val isPolicyAccepted: Flow<Boolean> = context.dataStore.data.map { it[KEY_POLICY_ACCEPTED] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BRIGHT_ENABLED] = enabled }
        if (enabled) {
            // PLUG-IN POINT: BrightSDK.start(context)
        } else {
            // PLUG-IN POINT: BrightSDK.stop(context)
        }
    }

    suspend fun acceptPolicy() {
        context.dataStore.edit { it[KEY_POLICY_ACCEPTED] = true }
    }

    fun getBonusMultiplier(): Double {
        val enabled = runBlocking { isEnabled.first() }
        // Base reward: +30% Hashrate Boost when active.
        return if (enabled) 1.30 else 1.0
    }

    fun getPrivacyText(): String {
        return """
            --- BRIGHT SDK PRIVACY POLICY ---
            By enabling Bright, you agree to share 
            your unused internet bandwidth to support 
            Etherion Network and earn significant rewards.
            
            1. SECURITY: All traffic is encrypted and routed through a secure gateway.
            2. PRIVACY: No personal data, browsing history, or private info is accessed.
            3. TRANSPARENCY: Only utilizes idle bandwidth when device is not under load.
            4. REWARD: Active Bright status grants a +30% boost to mining efficiency.
            ---------------------------------
        """.trimIndent()
    }
}
