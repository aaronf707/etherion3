package com.etherion.network.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.etherion.network.miner.MiningViewModel
import com.etherion.network.miner.MiningViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MiningViewModel = viewModel(factory = MiningViewModelFactory(context))
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    
    val scrollState = rememberScrollState()
    var showAvatarPicker by remember { mutableStateOf(false) }

    val levelColor = when {
        state.userLevel < 5 -> Color(0xFF00FF00) // Green
        state.userLevel < 10 -> Color(0xFF00FFFF) // Cyan
        state.userLevel < 20 -> Color(0xFFFFFF00) // Yellow
        else -> Color(0xFFFF00FF) // Purple/Magenta
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            ownedAvatars = state.ownedAvatars,
            balance = state.totalMined,
            onDismiss = { showAvatarPicker = false },
            onAvatarSelected = { avatarName ->
                scope.launch {
                    viewModel.updateSelectedAvatar(avatarName)
                    showAvatarPicker = false
                }
            },
            onAvatarPurchased = { avatarName, cost ->
                scope.launch {
                    viewModel.purchaseAvatar(avatarName, cost)
                }
            }
        )
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                    .clip(CircleShape)
                    .background(levelColor.copy(alpha = 0.1f))
                    .clickable { showAvatarPicker = true }
            ) {
                val avatarIcon = getAvatarIcon(state.profilePictureUrl)
                if (avatarIcon != null) {
                    Icon(
                        imageVector = avatarIcon,
                        contentDescription = "Selected Avatar",
                        modifier = Modifier.size(80.dp),
                        tint = levelColor
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = levelColor
                    )
                }
                
                // Edit Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp).padding(bottom = 8.dp)
                    )
                }
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
fun AvatarPickerDialog(
    ownedAvatars: List<String>,
    balance: Double,
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onAvatarPurchased: (String, Double) -> Unit
) {
    val avatars = listOf(
        AvatarData("Terminal", Icons.Default.Terminal, 0.0),
        AvatarData("Code", Icons.Default.Code, 5.0),
        AvatarData("Psychology", Icons.Default.Psychology, 5.0),
        AvatarData("Encryption", Icons.Default.EnhancedEncryption, 10.0),
        AvatarData("Lock", Icons.Default.LockOpen, 0.0),
        AvatarData("Bug", Icons.Default.BugReport, 5.0),
        AvatarData("Vpn", Icons.Default.VpnLock, 10.0),
        AvatarData("VisibilityOff", Icons.Default.VisibilityOff, 5.0),
        AvatarData("Memory", Icons.Default.Memory, 0.0),
        AvatarData("Hardware", Icons.Default.Hardware, 5.0),
        AvatarData("Construction", Icons.Default.Construction, 5.0),
        AvatarData("Factory", Icons.Default.PrecisionManufacturing, 15.0),
        AvatarData("Bolt", Icons.Default.Bolt, 0.0),
        AvatarData("Settings", Icons.Default.Settings, 0.0),
        AvatarData("Build", Icons.Default.Build, 5.0),
        AvatarData("Auto", Icons.Default.AutoMode, 10.0),
        AvatarData("Bitcoin", Icons.Default.CurrencyBitcoin, 50.0),
        AvatarData("Exchange", Icons.Default.CurrencyExchange, 20.0),
        AvatarData("Monetization", Icons.Default.MonetizationOn, 20.0),
        AvatarData("Wallet", Icons.Default.AccountBalanceWallet, 10.0),
        AvatarData("Savings", Icons.Default.Savings, 10.0),
        AvatarData("Token", Icons.Default.Token, 10.0),
        AvatarData("Money", Icons.Default.AttachMoney, 25.0),
        AvatarData("Diamond", Icons.Default.Diamond, 100.0),
        AvatarData("Dns", Icons.Default.Dns, 15.0),
        AvatarData("Router", Icons.Default.Router, 10.0),
        AvatarData("Lan", Icons.Default.Lan, 10.0),
        AvatarData("Rocket", Icons.Default.RocketLaunch, 25.0),
        AvatarData("Satellite", Icons.Default.SatelliteAlt, 25.0),
        AvatarData("Radar", Icons.Default.Radar, 15.0),
        AvatarData("Devices", Icons.Default.Devices, 10.0),
        AvatarData("Ad", Icons.Default.AdUnits, 10.0),
        AvatarData("Esports", Icons.Default.SportsEsports, 15.0),
        AvatarData("Asset", Icons.Default.VideogameAsset, 15.0),
        AvatarData("Gamepad", Icons.Default.Gamepad, 15.0),
        AvatarData("Casino", Icons.Default.Casino, 25.0),
        AvatarData("Adb", Icons.Default.Adb, 10.0),
        AvatarData("Sports", Icons.Default.SportsBar, 10.0),
        AvatarData("Pets", Icons.Default.Pets, 15.0),
        AvatarData("Rabbit", Icons.Default.CrueltyFree, 15.0),
        AvatarData("Bird", Icons.Default.FlutterDash, 15.0),
        AvatarData("Diversity", Icons.Default.Diversity1, 10.0),
        AvatarData("Heart", Icons.Default.Favorite, 10.0),
        AvatarData("Peace", Icons.Default.Public, 10.0),
        AvatarData("Sun", Icons.Default.WbSunny, 15.0),
        AvatarData("Moon", Icons.Default.NightsStay, 15.0),
        AvatarData("Star", Icons.Default.Star, 10.0),
        AvatarData("Cloud", Icons.Default.Cloud, 10.0),
        AvatarData("Flare", Icons.Default.Flare, 15.0),
        AvatarData("Brightness", Icons.Default.BrightnessHigh, 15.0),
        AvatarData("Explore", Icons.Default.Explore, 10.0),
        AvatarData("Water", Icons.Default.WaterDrop, 10.0),
        AvatarData("Air", Icons.Default.Air, 10.0),
        AvatarData("Fire", Icons.Default.Whatshot, 10.0),
        AvatarData("Flag", Icons.Default.Flag, 5.0),
        AvatarData("Globe", Icons.Default.Public, 5.0),
        AvatarData("Map", Icons.Default.Map, 5.0),
        AvatarData("Travel", Icons.Default.TravelExplore, 5.0),
        AvatarData("Person", Icons.Default.Person, 0.0),
        AvatarData("Shield", Icons.Default.Shield, 0.0),
        AvatarData("Hub", Icons.Default.Hub, 0.0),
        AvatarData("Security", Icons.Default.Security, 0.0)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("HARDWARE IDENTITY MARKET", color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().height(400.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(avatars) { data ->
                    val isOwned = ownedAvatars.contains(data.name)
                    val canAfford = balance >= data.cost
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isOwned) Color(0xFF004400).copy(alpha = 0.3f) else Color(0xFF101020))
                                .border(1.dp, if (isOwned) Color(0xFF00FF00) else Color.DarkGray, CircleShape)
                                .clickable { 
                                    if (isOwned) onAvatarSelected(data.name)
                                    else if (canAfford) onAvatarPurchased(data.name, data.cost)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                data.icon, 
                                contentDescription = data.name, 
                                tint = if (isOwned) Color(0xFF00FF00) else Color.Gray, 
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isOwned) "OWNED" else "${data.cost.toInt()} ETR",
                            color = if (isOwned) Color(0xFF00FF00) else if (canAfford) Color.White else Color.Red,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF050510)
    )
}

data class AvatarData(val name: String, val icon: ImageVector, val cost: Double)

fun getAvatarIcon(name: String?): ImageVector? {
    return when (name) {
        "Terminal" -> Icons.Default.Terminal
        "Code" -> Icons.Default.Code
        "Psychology" -> Icons.Default.Psychology
        "Encryption" -> Icons.Default.EnhancedEncryption
        "Lock" -> Icons.Default.LockOpen
        "Bug" -> Icons.Default.BugReport
        "Vpn" -> Icons.Default.VpnLock
        "VisibilityOff" -> Icons.Default.VisibilityOff
        "Memory" -> Icons.Default.Memory
        "Hardware" -> Icons.Default.Hardware
        "Construction" -> Icons.Default.Construction
        "Factory" -> Icons.Default.PrecisionManufacturing
        "Bolt" -> Icons.Default.Bolt
        "Settings" -> Icons.Default.Settings
        "Build" -> Icons.Default.Build
        "Auto" -> Icons.Default.AutoMode
        "Bitcoin" -> Icons.Default.CurrencyBitcoin
        "Exchange" -> Icons.Default.CurrencyExchange
        "Monetization" -> Icons.Default.MonetizationOn
        "Wallet" -> Icons.Default.AccountBalanceWallet
        "Savings" -> Icons.Default.Savings
        "Token" -> Icons.Default.Token
        "Money" -> Icons.Default.AttachMoney
        "Diamond" -> Icons.Default.Diamond
        "Dns" -> Icons.Default.Dns
        "Router" -> Icons.Default.Router
        "Lan" -> Icons.Default.Lan
        "Rocket" -> Icons.Default.RocketLaunch
        "Satellite" -> Icons.Default.SatelliteAlt
        "Radar" -> Icons.Default.Radar
        "Devices" -> Icons.Default.Devices
        "Ad" -> Icons.Default.AdUnits
        "Esports" -> Icons.Default.SportsEsports
        "Asset" -> Icons.Default.VideogameAsset
        "Gamepad" -> Icons.Default.Gamepad
        "Casino" -> Icons.Default.Casino
        "Adb" -> Icons.Default.Adb
        "Sports" -> Icons.Default.SportsBar
        "Pets" -> Icons.Default.Pets
        "Rabbit" -> Icons.Default.CrueltyFree
        "Bird" -> Icons.Default.FlutterDash
        "Diversity" -> Icons.Default.Diversity1
        "Heart" -> Icons.Default.Favorite
        "Peace" -> Icons.Default.Public
        "Sun" -> Icons.Default.WbSunny
        "Moon" -> Icons.Default.NightsStay
        "Star" -> Icons.Default.Star
        "Cloud" -> Icons.Default.Cloud
        "Flare" -> Icons.Default.Flare
        "Brightness" -> Icons.Default.BrightnessHigh
        "Explore" -> Icons.Default.Explore
        "Water" -> Icons.Default.WaterDrop
        "Air" -> Icons.Default.Air
        "Fire" -> Icons.Default.Whatshot
        "Flag" -> Icons.Default.Flag
        "Globe" -> Icons.Default.Public
        "Map" -> Icons.Default.Map
        "Travel" -> Icons.Default.TravelExplore
        "Person" -> Icons.Default.Person
        "Shield" -> Icons.Default.Shield
        "Hub" -> Icons.Default.Hub
        "Security" -> Icons.Default.Security
        else -> null
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
