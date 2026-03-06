package com.etherion.network.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
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
    var showReferralInput by remember { mutableStateOf(false) }
    
    // Referral Validation State
    var isCheckingCode by remember { mutableStateOf(false) }
    var isCodeValid by remember { mutableStateOf<Boolean?>(null) }

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

    // Auto-check referral code as the user types
    LaunchedEffect(referralCode) {
        val cleanCode = referralCode.uppercase().trim()
        if (cleanCode.length >= 8) {
            isCheckingCode = true
            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("referralCodes").document(cleanCode).get().await()
                isCodeValid = doc.exists()
            } catch (e: Exception) {
                isCodeValid = false
            }
            isCheckingCode = false
        } else {
            isCodeValid = null
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

            if (!isSignUp) {
                // LOGIN UI
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
            } else {
                // SIGN UP UI - Location & Referral
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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                    modifier = Modifier.fillMaxWidth()
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

                // REFERRAL SECTION - Only shown during sign up
                Spacer(modifier = Modifier.height(16.dp))
                
                if (showReferralInput) {
                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it },
                        label = { Text("Referral Code (Optional)", color = Color(0xFF00FFFF)) },
                        trailingIcon = {
                            if (isCheckingCode) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF00FFFF))
                            } else if (isCodeValid == true) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = Color.Green)
                            } else if (isCodeValid == false) {
                                Icon(Icons.Default.Error, contentDescription = "Invalid", tint = Color.Red)
                            }
                        },
                        supportingText = {
                            if (isCodeValid == true) {
                                Text("VALID REFERRAL CODE - 1.00 ETR BONUS ACTIVE", color = Color.Green, fontSize = 10.sp)
                            } else if (isCodeValid == false) {
                                Text("INVALID CODE - PLEASE CHECK AGAIN", color = Color.Red, fontSize = 10.sp)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = if (isCodeValid == true) Color.Green else if (isCodeValid == false) Color.Red else Color(0xFF00FFFF),
                            unfocusedIndicatorColor = if (isCodeValid == true) Color.Green.copy(alpha = 0.5f) else if (isCodeValid == false) Color.Red.copy(alpha = 0.5f) else Color(0xFF004444)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    TextButton(onClick = { showReferralInput = true }) {
                        Text("Have an invite code?", color = Color(0xFF00FFFF), fontSize = 12.sp)
                    }
                }
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
                onClick = { viewModel.signInWithGoogle(context, if (isSignUp && referralCode.isNotBlank() && isCodeValid == true) referralCode else null) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                enabled = !state.isLoading
            ) {
                Text("SIGN IN WITH GOOGLE", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { 
                isSignUp = !isSignUp 
                showReferralInput = false 
                referralCode = ""
                isCodeValid = null
            }) {
                Text(
                    text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                    color = Color(0xFF00FFC8)
                )
            }
        }
    }
}
