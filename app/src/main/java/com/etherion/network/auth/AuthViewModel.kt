package com.etherion.network.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etherion.network.referrals.ReferralManager
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
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
        val detectedRegion: String = "TIER_3"
    )

    private val _state = MutableStateFlow(AuthState(user = authManager.currentUser))
    val state: StateFlow<AuthState> = _state

    init {
        detectLocation()
    }

    fun detectLocation() {
        viewModelScope.launch {
            try {
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
                    detectedRegion = result.second
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Location detection failed", e)
                // Fallback to Locale
                val locale = Locale.getDefault()
                _state.value = _state.value.copy(
                    detectedCountry = locale.displayCountry + " (Estimated)",
                    detectedRegion = mapCountryToTier(locale.country)
                )
            }
        }
    }

    private fun mapCountryToTier(code: String): String {
        val tier1 = listOf("US", "CA", "GB", "AU", "NZ")
        val tier2 = listOf("AT", "BE", "DK", "FI", "FR", "DE", "IE", "IT", "LU", "NL", "NO", "PT", "ES", "SE", "CH", "JP", "KR", "SG", "BR")
        
        return when {
            tier1.contains(code.uppercase()) -> "TIER_1"
            tier2.contains(code.uppercase()) -> "TIER_2"
            else -> "TIER_3"
        }
    }

    fun signUp(email: String, password: String, referralCode: String? = null, context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            // Final verification during sign up
            val country = _state.value.detectedCountry
            val region = _state.value.detectedRegion

            val user = authManager.createUserWithEmail(email, password)
            if (user != null) {
                try {
                    val userData = mapOf(
                        "adRegion" to region,
                        "country" to country,
                        "email" to email,
                        "createdAt" to System.currentTimeMillis(),
                        "verificationMethod" to "IP_GEOLOCATION"
                    )
                    FirebaseFirestore.getInstance().collection("users").document(user.uid)
                        .set(userData, com.google.firebase.firestore.SetOptions.merge()).await()
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to save user info", e)
                }

                if (!referralCode.isNullOrBlank()) {
                    val referralManager = ReferralManager(context)
                    referralManager.applyReferrer(user.uid, referralCode)
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
            val user = authManager.signInWithGoogle(context)
            if (user != null) {
                if (!referralCode.isNullOrBlank()) {
                    val referralManager = ReferralManager(context)
                    referralManager.applyReferrer(user.uid, referralCode)
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
