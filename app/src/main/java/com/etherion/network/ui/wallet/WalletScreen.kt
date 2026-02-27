package com.etherion.network.ui.wallet

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.etherion.network.wallet.WalletViewModel
import com.etherion.network.wallet.WalletViewModelFactory
import com.etherion.network.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WalletScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    
    val walletVm: WalletViewModel = viewModel(factory = WalletViewModelFactory(context))
    val miningVm: MiningViewModel = viewModel(factory = MiningViewModelFactory(context))
    
    val walletState by walletVm.state.collectAsState()
    val miningState by miningVm.state.collectAsState()

    val adManager = remember { RewardedAdManager(context, context.getString(R.string.admob_rewarded_id)) }
    LaunchedEffect(Unit) { adManager.load() }

    Surface(
        color = Color(0xFF050510),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                "ETHERION WALLET",
                color = Color(0xFF00FF00),
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Address Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101020)),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF004400)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NODE ADDRESS", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        walletState.address,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Balance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101020)),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00FF00)))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CURRENT BALANCE", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        "${"%.4f".format(miningState.totalMined)} ETR",
                        color = Color(0xFF00FF00),
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { miningVm.claimDailyReward() },
                    modifier = Modifier.weight(1f),
                    enabled = miningState.canClaimDailyReward,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00), contentColor = Color.Black),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("DAILY REWARD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Withdrawal locked until Mainnet
                Button(
                    onClick = { /* Locked */ },
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(0xFF202030),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("MAINNET LOCKED", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Text(
                "Withdrawals will be enabled once the ETR token goes live on Mainnet.",
                color = Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "TRANSACTION HISTORY",
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider(color = Color(0xFF004400), modifier = Modifier.padding(vertical = 8.dp))

            if (walletState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00FF00))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(walletState.transactions) { tx ->
                        TransactionRow(tx)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: WalletViewModel.Transaction) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val date = dateFormat.format(Date(tx.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A15))
            .border(1.dp, Color(0xFF002200), RoundedCornerShape(4.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                tx.type.replace("_", " "),
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                date,
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            "+${"%.4f".format(tx.amount)} ETR",
            color = Color(0xFF00FF00),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
