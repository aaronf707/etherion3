package com.etherion.network.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.etherion.network.miner.MiningViewModel
import com.etherion.network.miner.MiningViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MiningViewModel = viewModel(factory = MiningViewModelFactory(context))
    val state by viewModel.state.collectAsState()
    
    val scrollState = rememberScrollState()
    
    val levelColor = when {
        state.userLevel < 5 -> Color(0xFF00FF00) // Green
        state.userLevel < 10 -> Color(0xFF00FFFF) // Cyan
        state.userLevel < 20 -> Color(0xFFFFFF00) // Yellow
        else -> Color(0xFFFF00FF) // Purple/Magenta
    }

    Surface(
        color = Color(0xFF050510),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Info, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "NODE IDENTITY",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Avatar Section
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .border(2.dp, levelColor, CircleShape)
                    .padding(8.dp)
                    .background(levelColor.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = levelColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                state.username,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            
            Surface(
                color = levelColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, levelColor)
            ) {
                Text(
                    state.rank,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = levelColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Progression Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("NODE LEVEL ${state.userLevel}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("LVL ${state.userLevel + 1}", color = Color.Gray, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simple calculation for next level progress
                val currentLevelBase = (state.userLevel - 1) * (state.userLevel - 1) * 5.0
                val nextLevelBase = state.userLevel * state.userLevel * 5.0
                val progress = ((state.totalMined - currentLevelBase) / (nextLevelBase - currentLevelBase)).toFloat().coerceIn(0f, 1f)
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = levelColor,
                    trackColor = Color.DarkGray
                )
                Text(
                    "Next Level: ${"%.2f".format(nextLevelBase - state.totalMined)} ETR required",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Career Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101020)),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF004400))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NODE CAREER METRICS", color = Color(0xFF00FF00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ProfileStatRow(Icons.Default.Token, "LIFETIME MINED", "${"%.4f".format(state.lifetimeMined)} ETR")
                    ProfileStatRow(Icons.Default.Speed, "MAX HASHRATE", "${"%.2f".format(state.maxHashrate)} H/s")
                    
                    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(state.joinedTimestamp))
                    ProfileStatRow(Icons.Default.History, "NODE ESTABLISHED", date)
                    ProfileStatRow(Icons.Default.Badge, "TEAM IMPACT", "${state.teamSize} ACTIVE NODES")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hardware Signature Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A15)),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = Color(0xFF00FFFF), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("HARDWARE SIGNATURE", color = Color(0xFF00FFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.equipment.tier.asciiArt,
                        color = Color(0xFF00FFFF).copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        state.equipment.tier.displayName.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileStatRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
