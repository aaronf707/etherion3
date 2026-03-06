package com.etherion.network.ui.team

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.etherion.network.domain.MiningEconomyManager
import com.etherion.network.miner.MiningViewModel
import com.etherion.network.miner.MiningViewModelFactory

@Composable
fun TeamHubScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MiningViewModel = viewModel(factory = MiningViewModelFactory(context))
    val state by viewModel.state.collectAsState()
    val economy = remember { MiningEconomyManager() }
    val clipboardManager = LocalClipboardManager.current
    
    val maxPossible = economy.calculateTheoreticalMax(state.equipment.tier)
    val efficiency = (state.hashrate / maxPossible).toFloat().coerceIn(0f, 1f)
    
    Surface(
        color = Color(0xFF050510),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "MINING TEAM HUB",
                    color = Color(0xFF00FF00),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // REFERRAL IDENTITY SECTION
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A15)),
                border = BorderStroke(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NODE CONNECTIONS", color = Color(0xFF00FFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // My Invite Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("YOUR INVITE CODE", color = Color.Gray, fontSize = 10.sp)
                            Text(state.myInviteCode, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                        }
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(state.myInviteCode)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF00FFFF), modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))
                    
                    // Referrer Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.referrerCode != null) "Established Link: ${state.referrerCode}" else "Uplink Status: Independent Node",
                            color = if (state.referrerCode != null) Color(0xFF00FF00) else Color.Gray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Efficiency Meter
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101020)),
                border = BorderStroke(1.dp, Color(0xFF00FF00).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TEAM PERFORMANCE METER", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${(efficiency * 100).toInt()}%", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EFFICIENCY", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { efficiency },
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                        color = if (efficiency > 0.8f) Color(0xFF00FF00) else Color(0xFFFFFF00),
                        trackColor = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Actual: ${"%.2f".format(state.hashrate)} H/s | Cap: ${"%.2f".format(maxPossible)} H/s",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TEAM MEMBER LIST
            Text("ACTIVE TEAM MEMBERS (${state.teamMembers.size})", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            if (state.teamMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A15), RoundedCornerShape(8.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No team members detected. Share your code to build your mining farm and boost your hashrate!", color = Color.Gray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                state.teamMembers.forEach { member ->
                    TeamMemberRow(member)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Breakdown Section
            Text("DETAILED HASHRATE BREAKDOWN", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            BreakdownRow("Base Hardware Output", state.equipment.tier.baseRate.toString() + "x", Icons.Default.Memory)
            BreakdownRow("Team Boost Multiplier", "+${state.teamSize * 10}% Speed", Icons.Default.Groups)
            BreakdownRow("Consistency Loyalty", "+${state.streak}% Boost", Icons.Default.Bolt)
            if (state.adBoostActive) BreakdownRow("Ad Overclock Boost", "1.5x Multiplier", Icons.Default.RocketLaunch)
            if (state.isDataSdkOptedIn) BreakdownRow("Research SDK Bonus", "+25% Bonus", Icons.Default.Dns)

            Spacer(modifier = Modifier.height(32.dp))

            // Optimization Suggestions
            Text("STRATEGY SUGGESTIONS", color = Color(0xFF00FFFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (state.teamSize < 10) {
                SuggestionCard(
                    "Recruit New Nodes",
                    "You have ${state.teamSize} members. Reach 10 members to hit a +100% Team Multiplier. Each member adds 10% more speed.",
                    Icons.Default.PersonAdd
                )
            }

            if (!state.isDataSdkOptedIn) {
                SuggestionCard(
                    "Enable Data Research",
                    "Node is missing the high-yield module. Enable Anonymous Data SDK in Settings for an instant +25% hashrate.",
                    Icons.Default.SettingsSuggest
                )
            }

            if (efficiency >= 0.95f) {
                SuggestionCard(
                    "Upgrade Hardware",
                    "Current equipment is at physical capacity. Upgrade your hardware tier in the Store to increase your hashrate ceiling.",
                    Icons.Default.Upgrade
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun TeamMemberRow(member: MiningViewModel.TeamMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF101020), RoundedCornerShape(8.dp))
            .border(1.dp, if (member.isMining) Color(0xFF00FF00).copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(if (member.isMining) Color(0xFF00FF00) else Color.Gray, CircleShape))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(member.username, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Level ${member.level} Node", color = Color.Gray, fontSize = 10.sp)
            }
        }
        Text(
            text = if (member.isMining) "MINING" else "IDLE",
            color = if (member.isMining) Color(0xFF00FF00) else Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun BreakdownRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00FF00), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, color = Color.LightGray, fontSize = 13.sp)
        }
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SuggestionCard(title: String, description: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A15)),
        border = BorderStroke(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00FFFF), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}
