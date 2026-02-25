package com.etherion.network.domain

import kotlin.math.max
import kotlin.math.min

class MiningEconomyManager {

    private val baseHashrate = 12.0
    val targetPriceMoonshot = 1.00 // $1.00 USD Moonshot
    val targetPriceStability = 0.15 // $0.15 USD Stability Target (Soft Target)

    // New Revenue Constants (Estimates for ETR backing)
    private val BANNER_ECPM_ESTIMATE = 0.50 // $0.50 per 1000 impressions
    private val DATA_SDK_MONTHLY_VALUE = 2.00 // $2.00 per user per month if opted-in
    private val REWARDED_AD_VALUE = 0.02 // $0.02 per rewarded ad view

    /**
     * Regional Multipliers based on Ad Revenue (eCPM) Tiers.
     * High: USA, CA, UK, AU, etc. (1.0x)
     * Mid: Europe, BR, etc. (0.6x)
     * Low: Rest of World (0.3x)
     */
    fun getRegionalMultiplier(region: String): Double {
        return when (region.uppercase()) {
            "TIER_1" -> 1.0 // High Revenue Regions
            "TIER_2" -> 0.6 // Medium Revenue Regions
            "TIER_3" -> 0.3 // Low Revenue Regions
            else -> 0.3
        }
    }

    /**
     * Calculates the Total Projected Revenue for the ecosystem.
     * Factors in: Rewarded Ads, 24/7 Banner Ads, and Data SDK participation.
     */
    fun calculateProjectedTotalRevenue(
        activeUsers: Long,
        avgRewardedAdsPerUser: Double,
        dataSdkOptInRate: Double
    ): Double {
        val rewardedRev = activeUsers * avgRewardedAdsPerUser * REWARDED_AD_VALUE
        val bannerRev = activeUsers * (BANNER_ECPM_ESTIMATE / 1000.0) * 24 * 12 // Assumes 12 impressions per hour
        val dataSdkRev = activeUsers * dataSdkOptInRate * (DATA_SDK_MONTHLY_VALUE / 30.0) // Daily value
        
        return rewardedRev + bannerRev + dataSdkRev
    }

    /**
     * Calculates the Revenue Multiplier.
     * Determines how much ETR can be mined based on 75% of total app revenue.
     * Uses the Stability Target for distribution to ensure we don't over-promise.
     */
    fun calculateRevenueMultiplier(totalRevenueUSD: Double, totalMinedETR: Double): Double {
        val userBudgetUSD = totalRevenueUSD * 0.75
        // Use stability price for safer distribution
        val userBudgetETR = userBudgetUSD / targetPriceStability
        
        if (userBudgetETR <= 0) return 0.1 // Minimal floor if no revenue yet
        
        val ratio = userBudgetETR / max(1.0, totalMinedETR)
        return ratio.coerceIn(0.1, 2.5)
    }

    private fun getTierCeiling(tier: MiningEquipmentTier): Double {
        return when (tier) {
            MiningEquipmentTier.CPU -> 30.0
            MiningEquipmentTier.GPU_ENTRY -> 75.0
            MiningEquipmentTier.GPU_PRO -> 180.0
            MiningEquipmentTier.RIG_SMALL -> 450.0
            MiningEquipmentTier.ASIC_STANDARD -> 1200.0
            MiningEquipmentTier.ASIC_PRO -> 3000.0
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
        integrity: Double = 1.0,
        adBoostMultiplier: Double = 1.0,
        revenueMultiplier: Double = 1.0,
        regionalMultiplier: Double = 1.0,
        dataSdkBoost: Boolean = false
    ): Double {
        val equipmentMultiplier = equipment.tier.baseRate
        val streakMultiplier = 1.0 + (max(0, minOf(100, streakDays)) * 0.01)
        
        // Data SDK provides a direct 25% hashrate boost for participating nodes
        val sdkMultiplier = if (dataSdkBoost) 1.25 else 1.0
        
        val rawHashrate = baseHashrate * 
               equipmentMultiplier * 
               streakMultiplier * 
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

    fun calculateEarnings(hashrate: Double): Double {
        // Standardized production rate: 0.000005 ETR per unit of hashrate per tick
        return hashrate * 0.000005
    }

    fun upgradeEquipment(current: MiningEquipment, targetTier: MiningEquipmentTier): MiningEquipment {
        return current.copy(tier = targetTier)
    }
    
    fun getAdTokenBoost(): Double = 0.05
    fun getReferralTokenBoost(): Double = 1.00
}
