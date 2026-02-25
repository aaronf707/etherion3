package com.etherion.network.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CustomBottomNavigationBar(
    onHomeClick: () -> Unit,
    onWalletClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // We use a Surface to ensure the background is correct and icons are visible
    Surface(
        color = Color(0xFF0A0A15),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Pushes the items up above the system bar
                .imePadding()           // Pushes the items up above the keyboard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp) // Standard bottom bar height
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onHomeClick) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "Home",
                        tint = Color(0xFF00FF00) // Explicit green tint
                    )
                }
                IconButton(onClick = onWalletClick) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Wallet",
                        tint = Color(0xFF00FF00) // Explicit green tint
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF00FF00) // Explicit green tint
                    )
                }
            }
        }
    }
}
