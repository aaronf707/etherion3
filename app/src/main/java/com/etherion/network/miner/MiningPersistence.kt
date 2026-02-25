package com.etherion.network.miner

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class MiningPersistence(private val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore("mining_store")
        private val KEY_BALANCE = doublePreferencesKey("balance")
        private val KEY_GUIDE_SHOWN = booleanPreferencesKey("guide_shown")
        private val KEY_AD_TIMESTAMPS = stringPreferencesKey("ad_timestamps")
        private val KEY_GUI_MODE = booleanPreferencesKey("gui_mode_enabled")
        private val KEY_LEGAL_ACCEPTED = booleanPreferencesKey("legal_accepted")
    }

    fun loadBalance(): Double = runBlocking {
        val prefs = context.dataStore.data.first()
        prefs[KEY_BALANCE] ?: 0.0
    }

    fun saveBalance(value: Double) = runBlocking {
        context.dataStore.edit { it[KEY_BALANCE] = value }
    }

    val isGuideShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GUIDE_SHOWN] ?: false
    }

    suspend fun setGuideShown(shown: Boolean) {
        context.dataStore.edit { it[KEY_GUIDE_SHOWN] = shown }
    }

    fun loadAdTimestamps(): List<Long> = runBlocking {
        val prefs = context.dataStore.data.first()
        val data = prefs[KEY_AD_TIMESTAMPS] ?: ""
        if (data.isEmpty()) emptyList() else data.split(",").mapNotNull { it.toLongOrNull() }
    }

    fun saveAdTimestamps(timestamps: List<Long>) = runBlocking {
        val data = timestamps.joinToString(",")
        context.dataStore.edit { it[KEY_AD_TIMESTAMPS] = data }
    }

    val isGuiModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GUI_MODE] ?: true
    }

    suspend fun setGuiModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GUI_MODE] = enabled }
    }

    val isLegalAccepted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LEGAL_ACCEPTED] ?: false
    }

    suspend fun setLegalAccepted(accepted: Boolean) {
        context.dataStore.edit { it[KEY_LEGAL_ACCEPTED] = accepted }
    }
}
