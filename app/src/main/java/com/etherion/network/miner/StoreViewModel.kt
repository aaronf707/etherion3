package com.etherion.network.miner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etherion.network.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StoreViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = AuthManager()

    data class StoreState(
        val tokenBalance: Double = 0.0,
        val currentTokenPrice: Double = 0.0,
        val estimatedPortfolioValue: Double = 0.0,
        val returnOnInvestment: Double = 0.0,
        val totalInvestment: Double = 0.0,
        val isLoading: Boolean = false
    )

    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state

    init {
        fetchMarketData()
        fetchUserBalance()
    }

    private fun fetchMarketData() {
        viewModelScope.launch {
            try {
                // Fetching token price from Firestore global config
                val marketDoc = db.collection("market").document("stats").get().await()
                val price = marketDoc.getDouble("currentTokenPrice") ?: 0.001 // Default floor price
                
                _state.value = _state.value.copy(currentTokenPrice = price)
                calculateROI()
            } catch (e: Exception) {
                Log.e("StoreViewModel", "Error fetching market data", e)
            }
        }
    }

    private fun fetchUserBalance() {
        val userId = auth.getUserId() ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                val balance = userDoc.getDouble("balance") ?: 0.0
                val investment = userDoc.getDouble("totalInvestment") ?: 0.0
                
                _state.value = _state.value.copy(
                    tokenBalance = balance,
                    totalInvestment = investment,
                    isLoading = false
                )
                calculateROI()
            } catch (e: Exception) {
                Log.e("StoreViewModel", "Error fetching user balance", e)
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private fun calculateROI() {
        val currentState = _state.value
        val portfolioValue = currentState.tokenBalance * currentState.currentTokenPrice
        
        // ROI % = ((Current Value - Total Investment) / Total Investment) * 100
        val roi = if (currentState.totalInvestment > 0) {
            ((portfolioValue - currentState.totalInvestment) / currentState.totalInvestment) * 100
        } else {
            0.0
        }

        _state.value = currentState.copy(
            estimatedPortfolioValue = portfolioValue,
            returnOnInvestment = roi
        )
    }

    fun refresh() {
        fetchMarketData()
        fetchUserBalance()
    }
}
