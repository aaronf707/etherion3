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
     * Gets the user's permanent referral code. 
     * Checks Firestore first, then generates and saves if it doesn't exist.
     */
    suspend fun getOrCreateReferralCode(userId: String): String {
        val userRef = db.collection("users").document(userId)
        val userDoc = userRef.get().await()
        
        val existingCode = userDoc.getString("myReferralCode")
        if (existingCode != null && existingCode.isNotBlank()) {
            return existingCode
        }

        // If no code exists in Firestore, generate a new one
        val newCode = generateCode()
        userRef.update("myReferralCode", newCode).await()
        
        // Also save it in the referralCodes collection for fast lookups
        val codeData = mapOf("uid" to userId, "code" to newCode)
        db.collection("referralCodes").document(newCode).set(codeData, SetOptions.merge()).await()
        
        return newCode
    }

    suspend fun applyReferrer(userId: String, code: String): Pair<Boolean, String> {
        val prefs = context.dataStore.data.first()
        if (prefs[KEY_REFERRER] != null) return Pair(false, "Referral already established.")

        val uppercaseCode = code.uppercase().trim()

        val codeDoc = db.collection("referralCodes").document(uppercaseCode).get().await()
        if (!codeDoc.exists()) return Pair(false, "Invalid referral code.")

        val referrerUid = codeDoc.getString("uid") ?: return Pair(false, "Invalid code mapping.")
        if (referrerUid == userId) return Pair(false, "Cannot refer yourself.")

        try {
            val batch = db.batch()

            // 1. Update Referee (Current User) - GIVE 1.0 ETR INSTANTLY
            val userRef = db.collection("users").document(userId)
            batch.set(userRef, mapOf(
                "balance" to FieldValue.increment(1.0),
                "referrerUid" to referrerUid
            ), SetOptions.merge())

            // 2. Record the link in the "referrals" collection
            val referralLogRef = db.collection("referrals").document(userId)
            val linkData = mapOf(
                "referrerUid" to referrerUid,
                "refereeUid" to userId,
                "code" to uppercaseCode,
                "bonusETR" to 1.0,
                "claimedByReferrer" to false,
                "timestamp" to System.currentTimeMillis()
            )
            batch.set(referralLogRef, linkData)

            // 3. Create a Notification for the Referrer
            // Get referee's name for the notification
            val refereeDoc = db.collection("users").document(userId).get().await()
            val refereeName = refereeDoc.getString("username") ?: "A new miner"
            
            val notificationRef = db.collection("users").document(referrerUid).collection("notifications").document()
            val notificationData = mapOf(
                "title" to "New Referral Established",
                "message" to "$refereeName has joined your team! Your hashrate has been boosted by +5%.",
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "type" to "REFERRAL"
            )
            batch.set(notificationRef, notificationData)

            batch.commit().await()

            context.dataStore.edit { it[KEY_REFERRER] = uppercaseCode }
            
            return Pair(true, "Success! 1.00 ETR Signup Bonus added to your node.")
        } catch (e: Exception) {
            return Pair(false, "Sync Error: ${e.message}")
        }
    }

    private fun generateCode(): String =
        (1..8).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
}
