package com.etherion.network.auth

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.etherion.network.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Observable flow of the current authenticated user.
     * Emits the current user immediately on subscription.
     */
    val userFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.onStart { emit(auth.currentUser) }

    suspend fun signInWithGoogle(context: Context): FirebaseUser? {
        val credentialManager = CredentialManager.create(context)
        val serverClientId = context.getString(R.string.default_web_client_id)
        
        Log.d("AuthManager", "Starting Google Sign-In with Client ID: $serverClientId")
        
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            Log.d("AuthManager", "Credential received successfully")
            handleSignIn(result)
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Google sign-in failed (GetCredentialException): ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e("AuthManager", "An unexpected error occurred: ${e.message}", e)
            null
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): FirebaseUser? {
        val credential = result.credential
        Log.d("AuthManager", "Handling sign-in for credential type: ${credential.type}")
        
        try {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                Log.d("AuthManager", "Firebase authentication successful")
                return authResult.user
            } else {
                Log.e("AuthManager", "Received credential is not a GoogleIdTokenCredential. Type: ${credential.type}")
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error parsing Google ID token credential", e)
        }
        return null
    }

    suspend fun signInAnonymously(): FirebaseUser? {
        return try {
            val result = auth.signInAnonymously().await()
            result.user
        } catch (e: Exception) {
            Log.e("AuthManager", "Anonymous sign-in failed")
            null
        }
    }

    suspend fun createUserWithEmail(email: String, password: String): FirebaseUser? {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign up failed")
            null
        }
    }

    suspend fun signInWithEmail(email: String, password: String): FirebaseUser? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign in failed")
            null
        }
    }

    suspend fun signInWithCredential(credential: AuthCredential): FirebaseUser? {
        return try {
            val result = auth.signInWithCredential(credential).await()
            result.user
        } catch (e: Exception) {
            Log.e("AuthManager", "Credential sign in failed")
            null
        }
    }

    suspend fun linkAnonymousAccount(email: String, password: String): Boolean {
        val user = auth.currentUser ?: return false
        val credential = EmailAuthProvider.getCredential(email, password)
        return try {
            user.linkWithCredential(credential).await()
            true
        } catch (e: Exception) {
            Log.e("AuthManager", "Linking failed")
            false
        }
    }

    suspend fun updateDisplayName(newName: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val profileUpdates = userProfileChangeRequest {
                displayName = newName
            }
            user.updateProfile(profileUpdates).await()
            true
        } catch (e: Exception) {
            Log.e("AuthManager", "Update profile failed")
            false
        }
    }

    suspend fun updatePhotoUri(uri: Uri): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val profileUpdates = userProfileChangeRequest {
                photoUri = uri
            }
            user.updateProfile(profileUpdates).await()
            true
        } catch (e: Exception) {
            Log.e("AuthManager", "Update photo failed")
            false
        }
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("AuthManager", "Failed to clear credential state")
        }
    }

    fun isUserSignedIn(): Boolean = currentUser != null
    fun getUserId(): String? = currentUser?.uid
}
