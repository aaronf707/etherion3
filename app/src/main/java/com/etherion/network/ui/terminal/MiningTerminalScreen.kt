package com.etherion.network.ui.terminal

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavController
import com.etherion.network.ads.RewardedAdManager
import com.etherion.network.miner.MiningViewModel
import com.etherion.network.miner.MiningViewModelFactory
import com.etherion.network.terminal.TerminalCommandEngine
import com.etherion.network.terminal.TerminalMiningBridge
import com.etherion.network.miner.MiningPersistence
import com.etherion.network.terminal.TerminalLine
import com.etherion.network.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MiningTerminalScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as Activity
    val miningViewModel: MiningViewModel = viewModel(factory = MiningViewModelFactory(context))
    val state by miningViewModel.state.collectAsState()
    val persistence = remember { MiningPersistence(context) }
    
    val isGuideShown by persistence.isGuideShown.collectAsState(initial = true)
    var showGuide by remember { mutableStateOf(false) }
    var isProcessingCommand by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(isGuideShown) {
        if (!isGuideShown) {
            showGuide = true
        }
    }

    if (showGuide) {
        GuideDialog(onDismiss = {
            showGuide = false
            scope.launch {
                persistence.setGuideShown(true)
            }
        })
    }

    val adManager = remember { RewardedAdManager(context, context.getString(R.string.admob_rewarded_id)) }
    LaunchedEffect(Unit) { adManager.load() }

    val cmdBuffer = miningViewModel.commandTerminalBuffer
    val mineBuffer = miningViewModel.miningTerminalBuffer
    val engine = remember { TerminalCommandEngine(cmdBuffer) }
    
    val bridge = remember(navController) { 
        TerminalMiningBridge(
            miningViewModel = miningViewModel, 
            buffer = cmdBuffer, 
            scope = scope,
            onWatchAd = { onComplete ->
                adManager.show(activity) {
                    onComplete()
                }
            },
            onNavigate = { route ->
                navController.navigate(route)
            }
        ) 
    }

    // LIST ALL COMMANDS ON FIRST START WITH IMPROVED VISUALS
    LaunchedEffect(Unit) {
        if (cmdBuffer.lines.value.isEmpty()) {
            cmdBuffer.append(TerminalLine("--- ETHERION NODE v1.0.4-BETA ---", TerminalLine.LineType.SUCCESS))
            cmdBuffer.append(TerminalLine("System: Welcome to the Etherion Network.", TerminalLine.LineType.NORMAL))
            cmdBuffer.append(TerminalLine("To run a command, type it into the green box below and press RUN.", TerminalLine.LineType.WARNING))
            
            cmdBuffer.append(TerminalLine("\n[BASIC COMMANDS]", TerminalLine.LineType.HEADER))
            cmdBuffer.append(TerminalLine("  mine start  - Starts your mining session (ad required)", TerminalLine.LineType.COMMAND_DETAIL))
            cmdBuffer.append(TerminalLine("  mine stop   - Safely halts all mining threads", TerminalLine.LineType.COMMAND_DETAIL))
            cmdBuffer.append(TerminalLine("  balance     - Checks your current ETR accumulation", TerminalLine.LineType.COMMAND_DETAIL))
            cmdBuffer.append(TerminalLine("  stats       - View hashrate and hardware performance", TerminalLine.LineType.COMMAND_DETAIL))
            cmdBuffer.append(TerminalLine("  repair      - Restore hardware integrity (costs ETR)", TerminalLine.LineType.COMMAND_DETAIL))
            
            cmdBuffer.append(TerminalLine("\n[CRYPTO & NETWORK]", TerminalLine.LineType.HEADER))
            cmdBuffer.append(TerminalLine("  tokenomics  - View supply cap and network burn data", TerminalLine.LineType.COMMAND_DETAIL))
            cmdBuffer.append(TerminalLine("  price       - View target launch value per ETR", TerminalLine.LineType.COMMAND_DETAIL))
            cmdBuffer.append(TerminalLine("  top         - List the highest performing nodes", TerminalLine.LineType.COMMAND_DETAIL))
            
            cmdBuffer.append(TerminalLine("\n[UTILITIES]", TerminalLine.LineType.HEADER))
            cmdBuffer.append(TerminalLine("  gui enable  - Switch to the graphical dashboard", TerminalLine.LineType.COMMAND_DETAIL))
            cmdBuffer.append(TerminalLine("  settings    - Open the node configuration menu", TerminalLine.LineType.COMMAND_DETAIL))
            cmdBuffer.append(TerminalLine("  help        - Redisplay this command directory", TerminalLine.LineType.COMMAND_DETAIL))
            
            cmdBuffer.append(TerminalLine("\n---------------------------------", TerminalLine.LineType.NORMAL))
            cmdBuffer.append(TerminalLine("PRO TIP: Start by typing 'mine start' below.", TerminalLine.LineType.SUCCESS))
        }
    }

    val cmdLines by cmdBuffer.lines.collectAsState()
    val mineLines by mineBuffer.lines.collectAsState()

    Surface(
        color = Color(0xFF050510),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("USER: ${state.username}", color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        Text("${"%.6f".format(state.totalMined)} ETR", color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    // LIVE NETWORK STATS KICKER
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NODES: ${state.totalDownloads}", color = Color(0xFF00FFFF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("ACTIVE: ${state.activeMiners}", color = Color(0xFF00FFFF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("ETR VALUE: $${"%.4f".format(state.projectedTokenValue)}", color = Color(0xFFFFCC00), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val hours = state.sessionTimeRemaining / (1000 * 60 * 60)
                        val mins = (state.sessionTimeRemaining / (1000 * 60)) % 60
                        val secs = (state.sessionTimeRemaining / 1000) % 60
                        Text("SESSION:", color = Color(0xFF00FF00), fontSize = 10.sp)
                        Text("%02d:%02d:%02d".format(hours, mins, secs), color = if (state.sessionTimeRemaining > 0) Color(0xFF00FF00) else Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("HASHRATE:", color = Color(0xFF00FF00), fontSize = 10.sp)
                        Text("${"%.2f".format(state.hashrate)} H/s", color = Color(0xFF00FF00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF004400))

                Text(" COMMAND CONSOLE", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                TerminalWindow(
                    lines = cmdLines,
                    onActionClick = { line ->
                        // Handle actions in command terminal if any (optional)
                    },
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                )

                HorizontalDivider(color = Color(0xFF004400), thickness = 2.dp)

                Text(" MINING OUTPUT STREAM", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                TerminalWindow(
                    lines = mineLines,
                    onActionClick = { line ->
                        val index = mineLines.indexOf(line)
                        if (index != -1) {
                            mineBuffer.updateLine(index) { it.copy(type = TerminalLine.LineType.DISABLED_LINK) }
                            
                            when (line.action) {
                                TerminalLine.TerminalAction.WATCH_AD -> {
                                    adManager.show(activity) { miningViewModel.applyAdReward() }
                                }
                                TerminalLine.TerminalAction.SOLVE_BLOCK -> {
                                    miningViewModel.solveBlockReward()
                                }
                                null -> {}
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                TerminalInputField(
                    onSubmit = { command ->
                        scope.launch {
                            isProcessingCommand = true
                            delay(600) // High-tech processing simulation
                            val cmd = command.trim().lowercase()
                            if (cmd.startsWith("mine") || cmd.startsWith("wallet") || cmd.startsWith("stats") || cmd.startsWith("upgrade") || 
                                cmd.startsWith("balance") || cmd.startsWith("reward") || cmd.startsWith("ref") || cmd.startsWith("ads") || 
                                cmd.startsWith("session") || cmd.startsWith("renew") || cmd.startsWith("logout") || cmd.startsWith("bright") ||
                                cmd.startsWith("gui") || cmd.startsWith("settings") || cmd.startsWith("top") || cmd.startsWith("tokenomics") ||
                                cmd.startsWith("roadmap") || cmd.startsWith("progress") || cmd.startsWith("network") || cmd.startsWith("news") ||
                                cmd.startsWith("price") || cmd.startsWith("policy") || cmd.startsWith("login") || cmd.startsWith("sign") ||
                                cmd.startsWith("help") || cmd.startsWith("clear") || cmd.startsWith("repair") || cmd.startsWith("share")) {
                                bridge.handle(command)
                            } else {
                                engine.execute(command)
                            }
                            isProcessingCommand = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Command Processing Overlay
            if (isProcessingCommand) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00FF00))
                }
            }
        }
    }
}
