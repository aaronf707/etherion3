package com.etherion.network.sync

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class ImageStorageManager {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadProfilePicture(userId: String, uri: Uri): String? {
        return try {
            val storageRef = storage.reference.child("profile_pictures/$userId.jpg")
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            Log.d("ImageStorageManager", "Image uploaded successfully: $downloadUrl")
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e("ImageStorageManager", "Upload failed: ${e.message}", e)
            null
        }
    }
}
