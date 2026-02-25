package com.etherion.network.rewards

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class RewardManager(private val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore("rewards_store")

        private val KEY_LAST_CLAIM = stringPreferencesKey("last_claim")
        private val KEY_STREAK = intPreferencesKey("streak")
    }

    data class RewardResult(
        val streak: Int,
        val reward: Double,
        val message: String
    )

    fun canClaimToday(): Flow<Boolean> = context.dataStore.data.map { prefs ->
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())
        prefs[KEY_LAST_CLAIM] != today
    }

    fun claimDaily(): RewardResult = runBlocking {
        val prefs = context.dataStore.data.first()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = dateFormat.format(Date())
        
        val lastClaim = prefs[KEY_LAST_CLAIM]
        val oldStreak = prefs[KEY_STREAK] ?: 0

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(calendar.time)

        val newStreak = when (lastClaim) {
            today -> oldStreak
            yesterday -> oldStreak + 1
            else -> 1
        }

        // 1.0 ETR base + streak bonus
        val reward = 1.0 + (newStreak * 0.1)

        if (lastClaim != today) {
            context.dataStore.edit {
                it[KEY_LAST_CLAIM] = today
                it[KEY_STREAK] = newStreak
            }
        }

        RewardResult(
            streak = newStreak,
            reward = reward,
            message = if (lastClaim == today) "Already claimed today!" else "Daily reward claimed!"
        )
    }
}
