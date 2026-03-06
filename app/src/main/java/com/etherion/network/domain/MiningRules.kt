package com.etherion.network.domain

object MiningRules {
    const val RULES_TEXT = """
        MINING HASHRATE & SCARCITY RULES
        
        1. THE EQUATION
        ETR Earned = (Hashrate * 0.000005) * Multipliers.
        A base hashrate of 20.0 H/s earns approx 0.36 ETR per hour.
        
        2. HARDWARE SCARCITY CEILINGS
        To prevent over-inflation, each tier has a physical hashrate limit:
        - CPU: 50 H/s
        - ENTRY GPU: 120 H/s
        - PRO GPU: 300 H/s
        - SMALL RIG: 800 H/s
        - STANDARD ASIC: 2,000 H/s
        - ENTERPRISE NODE: 5,000 H/s
        
        3. EQUIPMENT MULTIPLIER
        Upgrading hardware increases your base capability. 
        Higher tiers require Node Level progression and 
        Anonymous Data SDK participation.
        
        4. REFERRAL BOOST (TEAM GROWTH)
        Active team members provide a +5% boost per member.
        Build your mining farm to reach your hardware ceiling faster!
        
        5. MINING DIVIDENDS
        Earn 10% commission on all ETR mined by your team members.
        Dividends are automatically collected upon app launch.
        
        6. CONSISTENCY STREAK
        Each consecutive day of mining adds a +1% bonus (up to +100% total).
        
        7. NETWORK SUSTAINABILITY
        Emission rates are dynamically adjusted based on 75% of app revenue 
        to ensure ETR maintains a stable valuation target of ${'$'}0.15 USD.
        
        8. SESSION & BOOSTS
        Sessions last 4 hours. Engaging 'Bonus Ads' provides a 
        temporary +50% Overclock boost to your current hashrate.
    """
}
