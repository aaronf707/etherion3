package com.etherion.network.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class MiningEconomyManager {

    val baseHashrate = 12.0 // Reverted to original
    val targetPriceMoonshot = 1.00 
    val targetPriceStability = 0.15 

    private val GENESIS_TIMESTAMP = 1738368000000L 
    private val DAILY_DECAY_RATE = 0.015 

    private val BANNER_ECPM_ESTIMATE = 0.50
    private val DATA_SDK_MONTHLY_VALUE = 2.00
    private val REWARDED_AD_VALUE = 0.02

    fun calculateDailyDecayMultiplier(): Double {
        val currentTime = System.currentTimeMillis()
        val daysElapsed = (currentTime - GENESIS_TIMESTAMP) / (1000 * 60 * 60 * 24)
        if (daysElapsed <= 0) return 1.0
        return (1.0 - DAILY_DECAY_RATE).pow(daysElapsed.toDouble()).coerceAtLeast(0.01)
    }

    fun getRegionalMultiplier(region: String): Double {
        return when (region.uppercase()) {
            "TIER_1" -> 1.0
            "TIER_2" -> 0.6 // Reverted to original
            "TIER_3" -> 0.3 // Reverted to original
            else -> 0.3
        }
    }

    fun calculateProjectedTotalRevenue(
        activeUsers: Long,
        avgRewardedAdsPerUser: Double,
        dataSdkOptInRate: Double
    ): Double {
        val rewardedRev = activeUsers * avgRewardedAdsPerUser * REWARDED_AD_VALUE
        val bannerRev = activeUsers * (BANNER_ECPM_ESTIMATE / 1000.0) * 24 * 12 
        val dataSdkRev = activeUsers * dataSdkOptInRate * (DATA_SDK_MONTHLY_VALUE / 30.0)
        return rewardedRev + bannerRev + dataSdkRev
    }

    fun calculateRevenueMultiplier(totalRevenueUSD: Double, totalMinedETR: Double): Double {
        val userBudgetUSD = totalRevenueUSD * 0.75
        val userBudgetETR = userBudgetUSD / targetPriceStability
        if (userBudgetETR <= 0) return 0.1
        val ratio = userBudgetETR / max(1.0, totalMinedETR)
        return ratio.coerceIn(0.1, 2.5)
    }

    fun getTierCeiling(tier: MiningEquipmentTier): Double {
        return when (tier) {
            MiningEquipmentTier.CPU -> 30.0 // Reverted to original
            MiningEquipmentTier.GPU_ENTRY -> 75.0 // Reverted to original
            MiningEquipmentTier.GPU_PRO -> 180.0 // Reverted to original
            MiningEquipmentTier.RIG_SMALL -> 450.0 // Reverted to original
            MiningEquipmentTier.ASIC_STANDARD -> 1200.0 // Reverted to original
            MiningEquipmentTier.ASIC_PRO -> 3000.0 // Reverted to original
        }
    }

    fun getSessionScarcityCap(tier: MiningEquipmentTier): Double {
        return when (tier) {
            MiningEquipmentTier.CPU -> 2.16
            MiningEquipmentTier.GPU_ENTRY -> 5.40
            MiningEquipmentTier.GPU_PRO -> 12.96
            MiningEquipmentTier.RIG_SMALL -> 32.40
            MiningEquipmentTier.ASIC_STANDARD -> 86.40
            MiningEquipmentTier.ASIC_PRO -> 216.0
        }
    }

    fun calculateHashrate(
        equipment: MiningEquipment,
        streakDays: Int = 0,
        teamSize: Int = 0,
        integrity: Double = 1.0,
        adBoostMultiplier: Double = 1.0,
        revenueMultiplier: Double = 1.0,
        regionalMultiplier: Double = 1.0,
        dataSdkBoost: Boolean = false
    ): Double {
        val equipmentMultiplier = equipment.tier.baseRate
        val streakMultiplier = 1.0 + (max(0, minOf(100, streakDays)) * 0.01)
        val sdkMultiplier = if (dataSdkBoost) 1.25 else 1.0
        val teamMultiplier = 1.0 + (teamSize * 0.05)
        
        val rawHashrate = baseHashrate * 
               equipmentMultiplier * 
               streakMultiplier * 
               teamMultiplier *
               equipment.efficiency * 
               adBoostMultiplier *
               revenueMultiplier *
               regionalMultiplier *
               sdkMultiplier
        
        val ceiling = getTierCeiling(equipment.tier)
        var finalHashrate = min(rawHashrate, ceiling)

        if (integrity < 0.5) {
            val penalty = (integrity * 2.0).coerceIn(0.1, 1.0)
            finalHashrate *= penalty
        }
        
        return finalHashrate
    }

    fun calculateTheoreticalMax(tier: MiningEquipmentTier): Double {
        return getTierCeiling(tier)
    }

    fun calculateEarnings(hashrate: Double): Double {
        val decayMultiplier = calculateDailyDecayMultiplier()
        return hashrate * 0.000005 * decayMultiplier
    }

    fun upgradeEquipment(current: MiningEquipment, targetTier: MiningEquipmentTier): MiningEquipment {
        return current.copy(tier = targetTier)
    }
    
    fun getAdTokenBoost(): Double = 0.05
    fun getReferralCommissionRate(): Double = 0.10
}
