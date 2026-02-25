package com.etherion.network.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.etherion.network.domain.MiningEquipmentTier
import com.etherion.network.miner.MiningViewModel
import com.etherion.network.miner.MiningViewModelFactory
import com.etherion.network.miner.StoreViewModel
import androidx.compose.ui.platform.LocalContext

@Composable
fun StoreScreen() {
    val context = LocalContext.current
    val miningViewModel: MiningViewModel = viewModel(factory = MiningViewModelFactory(context))
    val storeViewModel: StoreViewModel = viewModel()
    
    val miningState by miningViewModel.state.collectAsState()
    val storeState by storeViewModel.state.collectAsState()

    val items = listOf(
        HardwareItem("GTX 1660 GPU", 10.0, MiningEquipmentTier.GPU_ENTRY, "Entry-level graphics processing. Requires Level 3 + Data SDK."),
        HardwareItem("RTX 4090 GPU", 35.0, MiningEquipmentTier.GPU_PRO, "Flagship consumer hardware. Requires Level 5 + Data SDK."),
        HardwareItem("Small Mining Rig", 150.0, MiningEquipmentTier.RIG_SMALL, "Custom rig with 6x GPUs. Requires Level 10 + Data SDK."),
        HardwareItem("Antminer S19", 500.0, MiningEquipmentTier.ASIC_STANDARD, "Dedicated ASIC miner. Requires Level 15 + Data SDK."),
        HardwareItem("Enterprise Node", 2000.0, MiningEquipmentTier.ASIC_PRO, "Full-scale server rack. Requires Level 20 + Data SDK.")
    )

    Surface(
        color = Color(0xFF050510),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "ETHERION HARDWARE DEPOT",
                color = Color(0xFF00FF00),
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Tokenomics Header
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101020)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00FFFF)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TokenStat("MARKET PRICE", "$${"%.4f".format(storeState.currentTokenPrice)}")
                        TokenStat("EST. ROI", "${"%.2f".format(storeState.returnOnInvestment)}%")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TokenStat("NODE CREDIT", "${"%.4f".format(miningState.totalMined)} ETR")
                        TokenStat("NODE LEVEL", "LVL ${miningState.userLevel}")
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(items) { item ->
                    HardwareCard(
                        item = item,
                        currentTier = miningState.equipment.tier,
                        balance = miningState.totalMined,
                        userLevel = miningState.userLevel,
                        isDataSdkEnabled = miningState.isDataSdkOptedIn,
                        onPurchase = { 
                            miningViewModel.upgradeEquipment(item.tier)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TokenStat(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color(0xFF00FFFF), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

data class HardwareItem(
    val name: String,
    val cost: Double,
    val tier: MiningEquipmentTier,
    val description: String
)

@Composable
fun HardwareCard(
    item: HardwareItem,
    currentTier: MiningEquipmentTier,
    balance: Double,
    userLevel: Int,
    isDataSdkEnabled: Boolean,
    onPurchase: () -> Unit
) {
    val currentOrdinal = currentTier.ordinal
    val itemOrdinal = item.tier.ordinal
    val isOwned = currentOrdinal >= itemOrdinal
    val canAfford = balance >= item.cost
    val levelMet = userLevel >= item.tier.levelRequirement
    val sdkMet = !item.tier.requiresDataSdk || isDataSdkEnabled

    val isEnabled = !isOwned && canAfford && levelMet && sdkMet

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101020)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isOwned) Color.Gray else if (isEnabled) Color(0xFF00FF00) else Color.Red.copy(alpha = 0.5f))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.name, color = if (isOwned) Color.Gray else Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("${item.cost} ETR", color = if (isOwned) Color.Gray else Color(0xFF00FF00), fontFamily = FontFamily.Monospace)
            }
            
            Text(
                item.description,
                color = if (isOwned) Color.DarkGray else Color.LightGray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (!isOwned) {
                if (!levelMet) {
                    Text("Requires Node Level ${item.tier.levelRequirement}", color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                if (!sdkMet) {
                    Text("Requires Anonymous Data SDK enabled in Settings", color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Button(
                onClick = onPurchase,
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOwned) Color.DarkGray else Color(0xFF00FF00),
                    disabledContainerColor = Color(0xFF202030)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttonText = when {
                    isOwned -> "INSTALLED"
                    !levelMet -> "LEVEL TOO LOW"
                    !sdkMet -> "SDK REQUIRED"
                    !canAfford -> "INSUFFICIENT FUNDS"
                    else -> "PURCHASE"
                }
                Text(
                    text = buttonText,
                    color = if (isEnabled) Color.Black else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
