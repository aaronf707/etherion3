package com.etherion.network.wallet

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WithdrawalManager {
    private val db = FirebaseFirestore.getInstance()

    data class WithdrawalRequest(
        val id: String = UUID.randomUUID().toString(),
        val userId: String = "",
        val amount: Double = 0.0,
        val address: String = "",
        val status: String = "PENDING",
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Places a withdrawal request into the processing queue.
     * Subject to level-based limits and human verification checks.
     */
    suspend fun requestWithdrawal(userId: String, amount: Double, address: String, userLevel: Int): Pair<Boolean, String> {
        // 1. Human Verification Check (Proof of Mining)
        if (userLevel < 5) {
            return Pair(false, "Human Verification Required: Reach Node Level 5 through active mining to enable withdrawals.")
        }

        // 2. Level-based limit check
        val maxWithdrawal = userLevel * 50.0 
        if (amount > maxWithdrawal) {
            return Pair(false, "Withdrawal exceeds limit for Level $userLevel (Max: $maxWithdrawal ETR). Upgrade level to increase limit.")
        }

        if (amount < 10.0) {
            return Pair(false, "Minimum withdrawal is 10.0 ETR.")
        }

        // 3. Check if user has an active pending request
        val activeRequests = db.collection("withdrawals")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "PENDING")
            .get().await()

        if (!activeRequests.isEmpty) {
            return Pair(false, "You already have a pending withdrawal request.")
        }

        // 4. Create request
        val request = WithdrawalRequest(userId = userId, amount = amount, address = address)
        
        return try {
            val userRef = db.collection("users").document(userId)
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentBalance = snapshot.getDouble("balance") ?: 0.0
                if (currentBalance < amount) {
                    throw Exception("Insufficient balance.")
                }
                transaction.update(userRef, "balance", currentBalance - amount)
                transaction.set(db.collection("withdrawals").document(request.id), request)
            }.await()

            Pair(true, "Request submitted. Funds are now locked in the processing queue. ID: ${request.id.take(8)}")
        } catch (e: Exception) {
            Pair(false, "Error: ${e.message}")
        }
    }

    suspend fun getWithdrawalHistory(userId: String): List<WithdrawalRequest> {
        return try {
            val snap = db.collection("withdrawals")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            snap.toObjects(WithdrawalRequest::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
