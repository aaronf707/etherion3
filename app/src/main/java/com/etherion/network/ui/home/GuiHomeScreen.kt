package com.etherion.network.ui.home

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.etherion.network.ads.RewardedAdManager
import com.etherion.network.miner.MiningViewModel
import com.etherion.network.miner.MiningViewModelFactory
import com.etherion.network.R
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun GuiHomeScreen(onNavigateToTeamOpt: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val miningViewModel: MiningViewModel = viewModel(factory = MiningViewModelFactory(context))
    val state by miningViewModel.state.collectAsState()

    val adManager = remember { RewardedAdManager(context, context.getString(R.string.admob_rewarded_id)) }
    LaunchedEffect(Unit) { adManager.load() }

    Surface(
        color = Color(0xFF050510),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            NetworkStatsKicker(state)

            Spacer(modifier = Modifier.height(12.dp))
            
            // THE LIVE MINING TERMINAL (Increased height for more rows)
            MiningTerminalLiveSimulation(state)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NODE DASHBOARD", color = Color(0xFF00FF00), fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Surface(
                    color = Color(0xFF00FF00).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF00))
                ) {
                    Text("LVL ${state.userLevel}", color = Color(0xFF00FF00), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = state.equipment.tier.asciiArt, color = Color(0xFF00FF00).copy(alpha = 0.7f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101020)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF00))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SESSION ACCUMULATION", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                        Text("%.6f".format(state.sessionEarnings), color = Color(0xFF00FF00), fontSize = 28.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ETR", color = Color(0xFF00FF00).copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    Text(if (state.isMining) "MINING THREADS ACTIVE" else "MINER STANDBY", color = if (state.isMining) Color(0xFF00FFFF) else Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("HARDWARE INTEGRITY", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    if (state.equipment.integrity < 0.9) {
                        TextButton(onClick = { miningViewModel.repairEquipment() }, modifier = Modifier.height(20.dp)) {
                            Text("REPAIR GEAR (0.50 ETR)", color = Color(0xFF00FFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                LinearProgressIndicator(progress = { state.equipment.integrity.toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp), color = if (state.equipment.integrity > 0.5) Color(0xFF00FF00) else Color.Red, trackColor = Color(0xFF333333))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoBox(
                    label = "HASHRATE", 
                    value = "${"%.2f".format(state.hashrate)} H/s", 
                    modifier = Modifier.weight(1f).clickable { onNavigateToTeamOpt() },
                    subValue = "View Boosts >"
                )
                Spacer(modifier = Modifier.width(16.dp))
                val hours = state.sessionTimeRemaining / (1000 * 60 * 60); val mins = (state.sessionTimeRemaining / (1000 * 60)) % 60; val secs = (state.sessionTimeRemaining / 1000) % 60
                InfoBox("SESSION", "%02d:%02d:%02d".format(hours, mins, secs), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A15)), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TOKEN BOOST", color = Color(0xFF00FFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Watch ad for +0.05 ETR", color = Color.Gray, fontSize = 10.sp)
                    }
                    Button(onClick = { adManager.show(activity) { miningViewModel.claimAdBonus() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF).copy(alpha = 0.1f)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFFF)), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF00FFFF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WATCH", color = Color(0xFF00FFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (state.isMining) miningViewModel.stopMining() else adManager.show(activity) { miningViewModel.startMining() } }, modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if (state.isMining) Color.Red.copy(alpha = 0.7f) else Color(0xFF00FF00)), shape = RoundedCornerShape(12.dp)) {
                    Text(if (state.isMining) "STOP MINING" else "ENGAGE MINER", color = if (state.isMining) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                }
                
                Button(
                    onClick = onNavigateToTeamOpt, 
                    modifier = Modifier.width(64.dp).height(56.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101020)), 
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFFF)), 
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Optimization", tint = Color(0xFF00FFFF))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            // THE LIVE TICKER (Taller and Marquee)
            NetworkTicker(state.tickerMessage)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MiningTerminalLiveSimulation(state: MiningViewModel.MiningState) {
    val terminalLines = remember { mutableStateListOf<String>() }
    val currentHashrate by rememberUpdatedState(state.hashrate)
    val currentActiveMiners by rememberUpdatedState(state.activeMiners)
    val totalMined by rememberUpdatedState(state.totalMined)
    
    LaunchedEffect(state.isMining) {
        if (state.isMining) {
            terminalLines.add("[NET] INITIALIZING STRATUM HANDSHAKE...")
            delay(800)
            terminalLines.add("[POOL] CONNECTED TO ETR-POOL-01 (USA)")
            delay(1000)
            terminalLines.add("[AUTH] NODE node_${state.username} VERIFIED")
            
            var lineCount = 0
            while (state.isMining) {
                delay(Random.nextLong(600, 2500))
                
                val displayLine = when {
                    lineCount % 8 == 0 -> "[STATS] ACTIVE NETWORK NODES: $currentActiveMiners"
                    lineCount % 12 == 0 -> "[STATS] TOTAL ETR SUPPLY: ${"%.2f".format(totalMined + 1420000.0)}"
                    lineCount % 15 == 0 -> "[STATS] ESTIMATED ETR BURNED: ${"%.4f".format(totalMined * 0.25)}"
                    lineCount % 5 == 0 -> "[STATUS] HASHRATE STABLE AT ${"%.2f".format(currentHashrate)} H/s"
                    else -> "[POOL] SHARE ACCEPTED FROM node_${state.username}"
                }
                
                terminalLines.add(displayLine)
                lineCount++
                
                // Increased to 20 lines for "more rows"
                if (terminalLines.size > 20) {
                    terminalLines.removeAt(0)
                }
            }
        } else {
            terminalLines.clear()
            terminalLines.add("[SYSTEM] MINER STANDBY")
            terminalLines.add("[NET] READY FOR SECURE CONNECTION")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp) // Increased height from 110dp to 160dp
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF004400), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            terminalLines.forEach { line ->
                Text(
                    text = "> $line",
                    color = when {
                        line.contains("ACCEPTED") -> Color(0xFF00FFC8)
                        line.contains("STATS") -> Color(0xFF00FFFF)
                        line.contains("node_") -> Color(0xFF00FF00)
                        else -> Color(0xFF00FF00).copy(alpha = 0.8f)
                    },
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun NetworkStatsKicker(state: MiningViewModel.MiningState) {
    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A15)).border(1.dp, Color(0xFF004400), RoundedCornerShape(4.dp)).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            KickerItem("TOTAL NODES", state.totalDownloads.toString())
            KickerItem("ACTIVE", state.activeMiners.toString())
            KickerItem("PROJ. VALUE", "$${"%.4f".format(state.projectedTokenValue)}")
        }
    }
}

@Composable
fun KickerItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color(0xFF00FFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetworkTicker(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101020))
            .border(1.dp, Color(0xFF004400))
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "LIVE:", 
                color = Color(0xFF00FFFF), 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold, 
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = message, 
                color = Color.Gray, 
                fontSize = 10.sp, 
                fontFamily = FontFamily.Monospace, 
                maxLines = 1,
                modifier = Modifier.basicMarquee() // Text streams across the screen
            )
        }
    }
}

@Composable
fun InfoBox(label: String, value: String, modifier: Modifier = Modifier, subValue: String? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF101020)), modifier = modifier, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF004400))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            if (subValue != null) {
                Text(subValue, color = Color(0xFF00FF00), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
