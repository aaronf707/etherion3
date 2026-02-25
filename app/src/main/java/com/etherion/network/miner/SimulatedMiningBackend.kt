package com.etherion.network.miner

import kotlin.random.Random

class SimulatedMiningBackend(
    private val baseRate: Double = 12.0
) : MiningBackend {

    override suspend fun tick(): MiningTickResult {
        val variance = Random.nextDouble(0.9, 1.1)
        val hashrate = baseRate * variance
        val earned = hashrate * 0.00001

        return MiningTickResult(
            hashrate = hashrate,
            earned = earned,
            status = "Mining..."
        )
    }
}
