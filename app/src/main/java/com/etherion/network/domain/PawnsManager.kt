package com.etherion.network.domain

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class PawnsManager(private val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore("pawns_store")
        private val KEY_PAWNS_ENABLED = booleanPreferencesKey("pawns_enabled")
        private val KEY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
    }

    val isEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_PAWNS_ENABLED] ?: false }
    val isPolicyAccepted: Flow<Boolean> = context.dataStore.data.map { it[KEY_POLICY_ACCEPTED] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PAWNS_ENABLED] = enabled }
        if (enabled) {
            // TODO: Start Pawns SDK background service
        } else {
            // TODO: Stop Pawns SDK background service
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
            --- PAWNS SDK PRIVACY POLICY ---
            By enabling this feature, you agree to share 
            your unused internet bandwidth to earn 
            additional ETR mining boosts.
            
            1. Your data is encrypted and secure.
            2. Only unused bandwidth is utilized.
            3. You can toggle this off at any time.
            4. This boost adds 25% to your base hashrate.
            -------------------------------
        """.trimIndent()
    }
}
