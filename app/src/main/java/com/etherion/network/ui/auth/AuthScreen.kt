package com.etherion.network.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.etherion.network.auth.AuthViewModel
import java.util.*

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val viewModel: AuthViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }

    val regions = listOf(
        "TIER_1" to "North America / UK / Australia (High Yield)",
        "TIER_2" to "Europe / Brazil / Asia (Mid Yield)",
        "TIER_3" to "Rest of World (Standard Yield)"
    )

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ETHERION NETWORK",
                color = Color(0xFF00FFC8),
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color(0xFF00FF00)) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF00FF00),
                    unfocusedIndicatorColor = Color(0xFF004400)
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color(0xFF00FF00)) },
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF00FF00),
                    unfocusedIndicatorColor = Color(0xFF004400)
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (isSignUp) {
                Spacer(modifier = Modifier.height(16.dp))

                // Location info - Read only to prevent lying
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101020)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FF00))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF00FF00))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Detected Location: ${state.detectedCountry}",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mining Tier: ${regions.find { it.first == state.detectedRegion }?.second ?: "Standard"}",
                            color = Color(0xFF00FFFF),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Location verified via IP geolocation to ensure fair yield distribution.",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = referralCode,
                    onValueChange = { referralCode = it },
                    label = { Text("Referral Code (Optional)", color = Color(0xFF00FFFF)) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF00FFFF),
                        unfocusedIndicatorColor = Color(0xFF004444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isSignUp) viewModel.signUp(email, password, referralCode, context)
                    else viewModel.signIn(email, password)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4EFF)),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (isSignUp) "CREATE ACCOUNT" else "LOGIN", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.signInWithGoogle(context, if (isSignUp) referralCode else null) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                enabled = !state.isLoading
            ) {
                Text("SIGN IN WITH GOOGLE", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(
                    text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                    color = Color(0xFF00FFC8)
                )
            }
        }
    }
}
