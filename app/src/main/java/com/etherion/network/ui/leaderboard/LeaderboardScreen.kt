package com.etherion.network.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val balance: Double,
    val streak: Int
)

@Composable
fun LeaderboardScreen() {
    val db = FirebaseFirestore.getInstance()
    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users")
            .orderBy("balance", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                entries = snap.documents.mapIndexed { index, doc ->
                    LeaderboardEntry(
                        rank = index + 1,
                        username = doc.getString("username") ?: "ANONYMOUS NODE",
                        balance = doc.getDouble("balance") ?: 0.0,
                        streak = (doc.getLong("streak") ?: 0L).toInt()
                    )
                }
            }
    }

    Surface(
        color = Color(0xFF050510),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFCC00),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "GLOBAL RANKING",
                        color = Color(0xFF00FF00),
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "TOP PERFORMING NODES",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Top 3 Highlights Row (Optional, could be added later)
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    LeaderboardItem(entry)
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(entry: LeaderboardEntry) {
    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFCC00) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color(0xFF00FF00).copy(alpha = 0.5f)
    }

    val isTop3 = entry.rank <= 3

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTop3) Color(0xFF101025) else Color(0xFF0A0A15)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isTop3) 1.dp else 0.5.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isTop3) listOf(rankColor, Color.Transparent) else listOf(Color.DarkGray, Color.Transparent)
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(rankColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, rankColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.rank.toString(),
                    color = rankColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.width(16.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.username.uppercase(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (entry.streak > 0) Color(0xFFFF4500) else Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "STREAK: ${entry.streak}D",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Balance
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%.3f".format(entry.balance),
                    color = Color(0xFF00FF00),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "ETR",
                    color = Color(0xFF00FF00).copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
