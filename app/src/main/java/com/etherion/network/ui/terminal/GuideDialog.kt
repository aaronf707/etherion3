package com.etherion.network.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun GuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF050510)),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00FF00))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "--- ETHERION NODE GUIDE ---",
                    color = Color(0xFF00FF00),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                GuideSection(
                    title = "1. COMMAND CONSOLE",
                    content = "The top terminal is where you interact with your node. Type 'help' to see all available commands. You can manage your wallet, view stats, and start/stop mining from here."
                )

                GuideSection(
                    title = "2. MINING OUTPUT",
                    content = "The bottom terminal shows real-time network activity. Watch for 'Share Accepted' messages to confirm you are earning ETR."
                )

                GuideSection(
                    title = "3. INTERACTIVE EVENTS",
                    content = "Keep an eye on the mining stream for clickable events:\n• [CLICK TO SOLVE]: Resolve a block for a large ETR reward.\n• [WATCH AD]: View a network sponsored ad for a bonus boost."
                )

                GuideSection(
                    title = "4. PERSISTENT MINING",
                    content = "Once you type 'mine start', your node stays active even if you close the app. Your earnings are calculated in the cloud based on server time."
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("INITIALIZE NODE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GuideSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            color = Color(0xFFFFCC00),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = content,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
