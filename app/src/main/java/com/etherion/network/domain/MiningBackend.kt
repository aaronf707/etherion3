package com.etherion.network.domain

import kotlin.random.Random

data class MiningTickResult(
    val hashrate: Double,
    val earned: Double,
    val status: String
)

interface MiningBackend {
    suspend fun tick(equipment: MiningEquipment): MiningTickResult
}

class SimulatedMiningBackend(
    private val economy: MiningEconomyManager,
    private val pawns: PawnsManager
) : MiningBackend {

    override suspend fun tick(equipment: MiningEquipment): MiningTickResult {
        // Calculate base hashrate from equipment
        val baseRate = economy.calculateHashrate(equipment)
        
        // Add random variance for realism
        val randomVariance = Random.nextDouble(0.95, 1.05)
        val currentHashrate = baseRate * randomVariance
        
        // Calculate earnings based on hashrate
        // Using a small constant to simulate "mining" rewards
        val earned = economy.calculateEarnings(currentHashrate)
        
        return MiningTickResult(
            hashrate = currentHashrate,
            earned = earned,
            status = "Mining (Simulated)"
        )
    }
}
