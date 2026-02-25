package com.etherion.network.domain

data class MiningEquipment(
    val tier: MiningEquipmentTier = MiningEquipmentTier.CPU,
    val efficiency: Double = 1.0,
    val integrity: Double = 1.0
)

enum class MiningEquipmentTier(
    val displayName: String,
    val baseRate: Double,
    val cost: Double,
    val levelRequirement: Int,
    val requiresDataSdk: Boolean,
    val asciiArt: String
) {
    CPU("Basic CPU", 1.0, 0.0, 1, false, """
        [  CPU  ]
         _______
        |   |   |
        |---|---|
        |___|___|
    """.trimIndent()),
    
    GPU_ENTRY("GTX 1660 GPU", 2.0, 10.0, 3, true, """
         _______
        |  [O]  |
        |_______|
        [==GPU==]
    """.trimIndent()),
    
    GPU_PRO("RTX 4090 GPU", 4.0, 35.0, 5, true, """
         ___________
        |  [O] [O]  |
        |___________|
        [==RTX-PRO==]
    """.trimIndent()),
    
    RIG_SMALL("Small Mining Rig", 8.0, 150.0, 10, true, """
         _________
        | [][][]  |
        | [][][]  |
        |_________|
        [ MINER-V1 ]
    """.trimIndent()),
    
    ASIC_STANDARD("Antminer S19", 12.0, 500.0, 15, true, """
         __________
        | [S-19]   |
        | (O)  (O) |
        |__________|
        [ ASIC-STD ]
    """.trimIndent()),
    
    ASIC_PRO("Enterprise Node", 20.0, 2000.0, 20, true, """
         __________
        |[  ||||  ]|
        |[  ||||  ]|
        |[________]|
        [ NODE-PRO ]
    """.trimIndent())
}
