package com.etherion.network.wallet

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etherion.network.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WalletViewModel(
    private val appContext: Context
) : ViewModel() {

    private val walletManager = WalletManager(appContext)
    private val auth = AuthManager()
    private val db = FirebaseFirestore.getInstance()
    private var transactionListener: ListenerRegistration? = null

    data class Transaction(
        val type: String = "",
        val amount: Double = 0.0,
        val timestamp: Long = 0L
    )

    data class WalletState(
        val address: String = "",
        val balance: Double = 0.0,
        val transactions: List<Transaction> = emptyList(),
        val isLoading: Boolean = false,
        val status: String = "Idle"
    )

    private val _state = MutableStateFlow(WalletState())
    val state: StateFlow<WalletState> = _state

    init {
        loadWallet()
        observeTransactions()
    }

    private fun loadWallet() {
        viewModelScope.launch {
            val wallet = walletManager.getOrCreateWallet()
            _state.value = _state.value.copy(
                address = wallet.address,
                status = "Wallet loaded"
            )
        }
    }

    private fun observeTransactions() {
        val userId = auth.getUserId() ?: return
        
        _state.value = _state.value.copy(isLoading = true)
        
        transactionListener?.remove()
        transactionListener = db.collection("users").document(userId).collection("rewards")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("WalletViewModel", "Listen failed", e)
                    _state.value = _state.value.copy(isLoading = false)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val txList = snapshot.toObjects(Transaction::class.java)
                    _state.value = _state.value.copy(
                        transactions = txList,
                        isLoading = false
                    )
                }
            }
    }

    override fun onCleared() {
        transactionListener?.remove()
        super.onCleared()
    }
}
