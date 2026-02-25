package com.etherion.network.domain

object MiningRules {
    const val RULES_TEXT = """
        MINING HASHRATE & SCARCITY RULES
        
        1. THE EQUATION
        ETR Earned = (Hashrate * 0.000005) * Multipliers.
        A base hashrate of 12.0 H/s earns approx 0.108 ETR per hour (0.432 per cycle).
        
        2. HARDWARE SCARCITY CEILINGS
        To prevent over-inflation, each tier has a physical hashrate limit:
        - CPU: 30 H/s
        - ENTRY GPU: 75 H/s
        - PRO GPU: 180 H/s
        - SMALL RIG: 450 H/s
        - STANDARD ASIC: 1,200 H/s
        - ENTERPRISE NODE: 3,000 H/s
        
        3. EQUIPMENT MULTIPLIER
        Upgrading hardware increases your base capability. 
        Higher tiers require Node Level progression and 
        Anonymous Data SDK participation.
        
        4. REFERRAL BOOST
        Active team members provide a +2% boost (up to +100% total).
        
        5. CONSISTENCY STREAK
        Each consecutive day of mining adds a +1% bonus (up to +100% total).
        
        6. NETWORK SUSTAINABILITY
        Emission rates are dynamically adjusted based on 75% of app revenue 
        to ensure ETR maintains a stable valuation target of ${'$'}0.15 USD.
        
        7. SESSION & BOOSTS
        Sessions last 4 hours. Engaging 'Bonus Ads' provides a 
        temporary +50% Overclock boost to your current hashrate.
    """
}
