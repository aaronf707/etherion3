package com.etherion.network.referrals

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class ReferralManager(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()

    companion object {
        private val Context.dataStore by preferencesDataStore("referral_store")
        private val KEY_REFERRAL_CODE = stringPreferencesKey("referral_code")
        private val KEY_REFERRER = stringPreferencesKey("referrer")
    }

    /**
     * Gets local code or generates a new one and registers it in Firestore.
     */
    suspend fun getOrCreateReferralCode(userId: String): String {
        val prefs = context.dataStore.data.first()
        var code = prefs[KEY_REFERRAL_CODE]

        if (code == null) {
            code = generateCode()
            context.dataStore.edit { it[KEY_REFERRAL_CODE] = code }
        }

        // Register/Update code in Firestore
        val codeData = mapOf(
            "uid" to userId,
            "code" to code
        )
        db.collection("referralCodes").document(code).set(codeData, SetOptions.merge()).await()
        
        return code
    }

    /**
     * Applies a referral code by verifying it in Firestore and linking the user.
     * Rewarded: Both parties receive a flat ETR bonus instead of a hashrate boost.
     */
    suspend fun applyReferrer(userId: String, code: String): Pair<Boolean, String> {
        val prefs = context.dataStore.data.first()
        if (prefs[KEY_REFERRER] != null) return Pair(false, "Referral already set.")

        val uppercaseCode = code.uppercase().trim()

        // 1. Check if code exists
        val codeDoc = db.collection("referralCodes").document(uppercaseCode).get().await()
        if (!codeDoc.exists()) return Pair(false, "Invalid referral code.")

        val referrerUid = codeDoc.getString("uid") ?: return Pair(false, "Invalid code mapping.")
        if (referrerUid == userId) return Pair(false, "Cannot refer yourself.")

        try {
            // 2. Atomic update: reward both parties with a flat ETR bonus
            val batch = db.batch()
            
            // Referrer gets a token bonus (1.0 ETR)
            val referrerRef = db.collection("users").document(referrerUid)
            batch.update(referrerRef, "balance", FieldValue.increment(1.0))
            
            // Referee also gets a token bonus (1.0 ETR)
            val refereeRef = db.collection("users").document(userId)
            batch.update(refereeRef, "balance", FieldValue.increment(1.0))
            
            // Log the referral link
            val referralLogRef = db.collection("referrals").document(userId)
            val referralData = mapOf(
                "referrer" to uppercaseCode,
                "referrerUid" to referrerUid,
                "refereeUid" to userId,
                "bonusETR" to 1.0,
                "timestamp" to System.currentTimeMillis()
            )
            batch.set(referralLogRef, referralData)
            
            batch.commit().await()

            // 3. Save link locally
            context.dataStore.edit { it[KEY_REFERRER] = uppercaseCode }
            
            return Pair(true, "Success! +1.00 ETR Bonus applied to both accounts.")
        } catch (e: Exception) {
            return Pair(false, "Sync Error: ${e.message}")
        }
    }

    private fun generateCode(): String =
        (1..8).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
}
