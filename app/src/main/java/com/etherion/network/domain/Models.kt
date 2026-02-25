package com.etherion.network.domain

data class Transaction(
    val id: String,
    val amount: Double,
    val timestamp: Long,
    val type: TransactionType
)

enum class TransactionType {
    MINING_REWARD,
    REFERRAL_REWARD,
    PREMIUM_PURCHASE,
    AD_REWARD
}

data class ReferralStats(
    val referralCount: Int = 0,
    val bonusMultiplier: Double = 1.0
)

data class NetworkStats(
    val totalHashRate: Double = 0.0,
    val activeMiners: Int = 0,
    val tokenValue: Double = 0.0,
    val pawnsActiveNodes: Int = 0
)

data class Boost(
    val id: String,
    val multiplier: Double,
    val expiresAt: Long
)

data class PawnsStatus(
    val isEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val lastEarnings: Double = 0.0
)
