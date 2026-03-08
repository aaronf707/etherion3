package com.etherion.network.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etherion.network.referrals.ReferralManager
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class AuthViewModel(private val authManager: AuthManager = AuthManager()) : ViewModel() {

    data class AuthState(
        val isLoading: Boolean = false,
        val user: FirebaseUser? = null,
        val error: String? = null,
        val isSuccess: Boolean = false,
        val detectedCountry: String = "Detecting...",
        val detectedRegion: String = "TIER_3",
        val isLocationReady: Boolean = false
    )

    private val _state = MutableStateFlow(AuthState(user = authManager.currentUser))
    val state: StateFlow<AuthState> = _state

    private var detectionJob: Deferred<Pair<String, String>>? = null

    init {
        detectionJob = viewModelScope.async { performLocationDetection() }
    }

    private suspend fun performLocationDetection(): Pair<String, String> {
        return try {
            val result = withContext(Dispatchers.IO) {
                val url = URL("https://ipapi.co/json/")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                val countryCode = json.optString("country_code", "")
                val countryName = json.optString("country_name", "Unknown")
                
                val region = mapCountryToTier(countryCode)
                Pair(countryName, region)
            }
            _state.value = _state.value.copy(
                detectedCountry = result.first,
                detectedRegion = result.second,
                isLocationReady = true
            )
            result
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Location detection failed", e)
            val locale = Locale.getDefault()
            val fallback = Pair(locale.displayCountry + " (Estimated)", mapCountryToTier(locale.country))
            _state.value = _state.value.copy(
                detectedCountry = fallback.first,
                detectedRegion = fallback.second,
                isLocationReady = true
            )
            fallback
        }
    }

    private fun mapCountryToTier(code: String): String {
        // Expanded list for better accuracy
        val tier1 = listOf("US", "USA", "CA", "CAN", "GB", "GBR", "AU", "AUS", "NZ", "NZL")
        val tier2 = listOf("AT", "BE", "DK", "FI", "FR", "DE", "IE", "IT", "LU", "NL", "NO", "PT", "ES", "SE", "CH", "JP", "KR", "SG", "BR")
        
        val cleanCode = code.uppercase().trim()
        return when {
            tier1.contains(cleanCode) -> "TIER_1"
            tier2.contains(cleanCode) -> "TIER_2"
            else -> "TIER_3"
        }
    }

    fun signUp(email: String, password: String, referralCode: String? = null, context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val location = detectionJob?.await() ?: Pair("Unknown", "TIER_3")
            val country = location.first
            val region = location.second

            val user = authManager.createUserWithEmail(email, password)
            if (user != null) {
                try {
                    val userData = mapOf(
                        "adRegion" to region,
                        "country" to country,
                        "email" to email,
                        "balance" to 0.0,
                        "createdAt" to System.currentTimeMillis(),
                        "joinedTimestamp" to System.currentTimeMillis(),
                        "verificationMethod" to "IP_GEOLOCATION"
                    )
                    FirebaseFirestore.getInstance().collection("users").document(user.uid)
                        .set(userData, SetOptions.merge()).await()

                    if (!referralCode.isNullOrBlank()) {
                        val referralManager = ReferralManager(context)
                        referralManager.applyReferrer(user.uid, referralCode)
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to save user info", e)
                }

                _state.value = _state.value.copy(isLoading = false, user = user, isSuccess = true)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val user = authManager.signInWithEmail(email, password)
            if (user != null) {
                _state.value = _state.value.copy(isLoading = false, user = user, isSuccess = true)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = "Sign in failed")
            }
        }
    }

    fun signInWithGoogle(context: Context, referralCode: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            val location = detectionJob?.await() ?: Pair("Unknown", "TIER_3")
            
            val user = authManager.signInWithGoogle(context)
            if (user != null) {
                try {
                    val userDoc = FirebaseFirestore.getInstance().collection("users").document(user.uid).get().await()
                    if (!userDoc.exists()) {
                        val userData = mapOf(
                            "adRegion" to location.second,
                            "country" to location.first,
                            "email" to user.email,
                            "balance" to 0.0,
                            "createdAt" to System.currentTimeMillis(),
                            "joinedTimestamp" to System.currentTimeMillis(),
                            "verificationMethod" to "GOOGLE_OAUTH"
                        )
                        FirebaseFirestore.getInstance().collection("users").document(user.uid)
                            .set(userData, SetOptions.merge()).await()
                    }

                    if (!referralCode.isNullOrBlank()) {
                        val referralManager = ReferralManager(context)
                        referralManager.applyReferrer(user.uid, referralCode)
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed during Google sign-in sync", e)
                }

                _state.value = _state.value.copy(isLoading = false, user = user, isSuccess = true)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = "Google Sign-In failed")
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authManager.signOut(context)
            _state.value = AuthState()
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
