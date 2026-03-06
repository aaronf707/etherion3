package com.etherion.network.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etherion.network.miner.MiningViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationDialog(
    notifications: List<MiningViewModel.Notification>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF00FF00))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "NODE ALERTS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active alerts detected.", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(notifications) { notif ->
                            NotificationItem(notif)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF101020),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun NotificationItem(notification: MiningViewModel.Notification) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF050510)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            val icon = if (notification.type == "REFERRAL") Icons.Default.GroupAdd else Icons.Default.Info
            val iconColor = if (notification.type == "REFERRAL") Color(0xFF00FFFF) else Color(0xFF00FF00)
            
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    notification.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    notification.message,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                val date = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(notification.timestamp))
                Text(
                    date,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
