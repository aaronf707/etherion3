package com.etherion.network.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
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
import com.etherion.network.miner.MiningViewModel
import com.etherion.network.miner.MiningViewModelFactory
import com.etherion.network.domain.MiningRules
import com.etherion.network.domain.MiningAudit
import com.etherion.network.domain.MainnetPolicy
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onViewProfile: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MiningViewModel = viewModel(factory = MiningViewModelFactory(context))
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    
    val scrollState = rememberScrollState()
    
    var showRules by remember { mutableStateOf(false) }
    var showAudit by remember { mutableStateOf(false) }
    var showPolicy by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("Loading...") }

    LaunchedEffect(state.userId) {
        if (state.userId != null) {
            try {
                inviteCode = viewModel.getReferralCode()
            } catch (e: Exception) {
                inviteCode = "ERROR"
            }
        }
    }

    Surface(
        color = Color(0xFF050510),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                "SETTINGS",
                color = Color(0xFF00FF00),
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PROFILE BUTTON
            Button(
                onClick = onViewProfile,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00).copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, Color(0xFF00FF00))
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00FF00))
                Spacer(modifier = Modifier.width(8.dp))
                Text("VIEW NODE IDENTITY", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ACCOUNT SECTION
            SettingHeader("ACCOUNT")
            Text("User ID: ${state.userId ?: "N/A"}", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text("Email: ${FirebaseAuth.getInstance().currentUser?.email ?: "Guest Session"}", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text("Username: ${state.username}", color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Invite Code: $inviteCode", color = Color(0xFF00FFFF), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showUsernameDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("CHANGE USERNAME", color = Color.White)
                }
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Etherion Mining Team")
                            putExtra(Intent.EXTRA_TEXT, "Join my mining team on Etherion! Use my code $inviteCode to get a 1.00 ETR signup bonus. Download now and start earning!")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share your invite code"))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF), contentColor = Color.Black)
                ) {
                    Text("SHARE INVITE", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* Implement delete confirmation */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.5f))
            ) {
                Text("DELETE ACCOUNT", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SYSTEM SECTION
            SettingHeader("SYSTEM")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Background Mining", color = Color.White)
                Switch(checked = true, onCheckedChange = {}, enabled = false, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF00)))
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Anonymous Data SDK", color = Color.White)
                    Text("Boost hashrate by +25% and earn passive ETR.", color = Color.Gray, fontSize = 10.sp)
                }
                Switch(
                    checked = state.isDataSdkOptedIn,
                    onCheckedChange = { _ ->
                        scope.launch { viewModel.syncAllDataToFirebase() }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF00), checkedTrackColor = Color(0xFF004400))
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00FF00)))
            ) {
                Text("OPTIMIZE BATTERY", color = Color(0xFF00FF00))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ECONOMICS SECTION
            SettingHeader("ECONOMICS")
            TextButton(onClick = { showRules = true }) {
                Text("Mining Hashrate Rules", color = Color(0xFF00FFFF))
            }
            TextButton(onClick = { showAudit = true }) {
                Text("View Economic Audit", color = Color(0xFF00FFFF))
            }
            TextButton(onClick = { showPolicy = true }) {
                Text("Mainnet & Withdrawal Policy", color = Color(0xFF00FFFF))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SUPPORT SECTION
            SettingHeader("SUPPORT")
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@etherion.network")
                        putExtra(Intent.EXTRA_SUBJECT, "Etherion Support Request [Node: ${state.username}]")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle case where no email app is installed
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202030)),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00FFFF)))
            ) {
                Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF00FFFF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CONTACT SUPPORT", color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TECHNICAL SECTION
            SettingHeader("TECHNICAL")
            Text("Node Version: ${state.nodeVersion}", color = Color.Gray, fontSize = 12.sp)
            Text("Network Diff: 1.42 TH", color = Color.Gray, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Text("LOGOUT", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Dialogs...
    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            title = { Text("Mining Rules", color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    MiningRules.RULES_TEXT,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { showRules = false }) {
                    Text("CLOSE", color = Color(0xFF00FF00))
                }
            },
            containerColor = Color(0xFF101020)
        )
    }

    if (showAudit) {
        AlertDialog(
            onDismissRequest = { showAudit = false },
            title = { Text("Economic Audit", color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    MiningAudit.AUDIT_TEXT,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { showAudit = false }) {
                    Text("CLOSE", color = Color(0xFF00FF00))
                }
            },
            containerColor = Color(0xFF101020)
        )
    }

    if (showPolicy) {
        AlertDialog(
            onDismissRequest = { showPolicy = false },
            title = { Text("Mainnet & Withdrawal Policy", color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace) },
            text = {
                Text(
                    MainnetPolicy.POLICY_TEXT,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { showPolicy = false }) {
                    Text("CLOSE", color = Color(0xFF00FF00))
                }
            },
            containerColor = Color(0xFF101020)
        )
    }

    if (showUsernameDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("Change Username", color = Color(0xFF00FF00)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Username (3-15 Chars)", color = Color(0xFF00FF00)) },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Color(0xFF00FF00))
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (viewModel.updateUsername(newName)) {
                                showUsernameDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00))
                ) {
                    Text("CONFIRM", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun SettingHeader(title: String) {
    Column {
        Text(title, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        HorizontalDivider(color = Color(0xFF004400), modifier = Modifier.padding(vertical = 8.dp))
    }
}
