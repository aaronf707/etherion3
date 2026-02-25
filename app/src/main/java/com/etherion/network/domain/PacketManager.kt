package com.etherion.network.domain

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class PacketManager(private val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore("packet_store")
        private val KEY_PACKET_ENABLED = booleanPreferencesKey("packet_enabled")
        private val KEY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
    }

    val isEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_PACKET_ENABLED] ?: false }
    val isPolicyAccepted: Flow<Boolean> = context.dataStore.data.map { it[KEY_POLICY_ACCEPTED] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PACKET_ENABLED] = enabled }
        if (enabled) {
            // Logic to start Packet SDK service would go here
        } else {
            // Logic to stop Packet SDK service would go here
        }
    }

    suspend fun acceptPolicy() {
        context.dataStore.edit { it[KEY_POLICY_ACCEPTED] = true }
    }

    fun getBonusMultiplier(): Double {
        val enabled = runBlocking { isEnabled.first() }
        return if (enabled) 1.25 else 1.0 // 25% Hashrate Boost
    }

    fun getPrivacyText(): String {
        return """
            --- PACKET SDK PRIVACY POLICY ---
            By enabling Packet, you share unused 
            network bandwidth to support the 
            Etherion Network and earn boosts.
            
            1. SECURITY: All traffic is encrypted.
            2. IMPACT: Only utilizes idle bandwidth.
            3. CONTROL: You can disable this anytime.
            4. REWARD: Active Packet earns +25% H/s.
            ---------------------------------
        """.trimIndent()
    }
}
