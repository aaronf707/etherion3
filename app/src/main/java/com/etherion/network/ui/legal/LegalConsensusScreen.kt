package com.etherion.network.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.etherion.network.miner.MiningPersistence
import kotlinx.coroutines.launch

@Composable
fun LegalConsensusScreen(onAccepted: () -> Unit) {
    val context = LocalContext.current
    val persistence = remember { MiningPersistence(context) }
    val scope = rememberCoroutineScope()
    var checked by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF050510),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // This fixes the top being cut off by the notch/status bar
                .navigationBarsPadding() // Ensures the bottom button isn't cut off by gesture pill
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ETHERION NETWORK",
                color = Color(0xFF00FF00),
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                "OFFICIAL TERMS & PRIVACY",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF101020))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    LegalSection("1. USER AGREEMENT", "By establishing a node on the Etherion Network, you agree to participate in a distributed decentralized mining ecosystem. ETR is a utility token used within this application to represent network contribution and node tier standing. Participation is voluntary and does not guarantee financial return.")
                    LegalSection("2. DATA PRIVACY & USAGE", "We prioritize your privacy. We collect: (a) Authentication data via Google/Firebase to secure your account; (b) Anonymous node performance metrics to maintain network stability; (c) Optional device telemetry if you opt into the Data Research SDK. We do NOT sell personally identifiable information to third parties.")
                    LegalSection("3. MONETIZATION CONSENT", "Etherion Network is a revenue-backed ecosystem. By mining, you consent to the display of Rewarded and Banner advertisements. 75% of the revenue generated is used to provide liquidity, backing, and deflationary 'burn' mechanisms for the ETR token.")
                    LegalSection("4. ANTI-FRAUD POLICY", "To maintain ecosystem integrity, any use of automated bots, scripts, or multiple accounts on a single device to manipulate hashrates is strictly prohibited. Detected fraudulent activity will result in immediate node suspension and forfeiture of all accumulated ETR.")
                    LegalSection("5. SERVICE AVAILABILITY", "Etherion Network reserves the right to adjust emission rates, hashrate ceilings, and reward structures to ensure long-term economic stability. The Mainnet migration is projected for Q4 2025, pending network scale targets.")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FF00))
                )
                Text(
                    "I accept the Terms of Service and Privacy Policy.",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        persistence.setLegalAccepted(true)
                        onAccepted()
                    }
                },
                enabled = checked,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF00),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                Text("ESTABLISH CONSENSUS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LegalSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(title, color = Color(0xFF00FF00), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        Text(content, color = Color.LightGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
