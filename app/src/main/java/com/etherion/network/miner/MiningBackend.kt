package com.etherion.network.miner

interface MiningBackend {
    suspend fun tick(): MiningTickResult
}

data class MiningTickResult(
    val hashrate: Double,
    val earned: Double,
    val status: String
)
