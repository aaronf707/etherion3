package com.etherion.network.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class SyncManager {
    private val db = FirebaseFirestore.getInstance()

    suspend fun uploadUserData(userId: String, data: Map<String, Any>) {
        try {
            db.collection("users").document(userId)
                .set(data, SetOptions.merge())
                .await()
            Log.d("SyncManager", "Data uploaded successfully for $userId")
        } catch (e: Exception) {
            Log.e("SyncManager", "Upload failed for $userId: ${e.message}", e)
        }
    }

    suspend fun downloadUserData(userId: String): Map<String, Any>? {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            Log.d("SyncManager", "Data downloaded for $userId")
            snapshot.data
        } catch (e: Exception) {
            Log.e("SyncManager", "Download failed for $userId: ${e.message}", e)
            null
        }
    }

    suspend fun updateUsername(userId: String, username: String) {
        uploadUserData(userId, mapOf("username" to username))
    }
}
