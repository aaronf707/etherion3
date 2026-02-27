package com.etherion.network.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etherion.network.R
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val logoPainter = painterResource(id = R.drawable.etr)
    
    var glitchTrigger by remember { mutableIntStateOf(0) }
    var bootText by remember { mutableStateOf("INITIALIZING CORE...") }
    
    LaunchedEffect(Unit) {
        // Glitch loop
        while(true) {
            delay(Random.nextLong(300, 1500))
            glitchTrigger++
            delay(100)
            glitchTrigger++
        }
    }

    LaunchedEffect(Unit) {
        delay(800)
        bootText = "LOAD PROTOCOL: ETR_v1.1.0"
        delay(800)
        bootText = "ESTABLISHING ENCRYPTED NODE..."
        delay(800)
        bootText = "DECRYPTING MARKET DATA..."
        delay(600)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510)),
        contentAlignment = Alignment.Center
    ) {
        // Decorative binary background
        BinaryRainBackground()

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            if (glitchTrigger % 2 != 0) {
                // Glitch layer 1 (Cyan offset)
                Image(
                    painter = logoPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = (-4).dp, y = 2.dp),
                    colorFilter = ColorFilter.tint(Color(0xFF00FFFF).copy(alpha = 0.5f))
                )
                // Glitch layer 2 (Magenta offset)
                Image(
                    painter = logoPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = 4.dp, y = (-2).dp),
                    colorFilter = ColorFilter.tint(Color(0xFFFF00FF).copy(alpha = 0.5f))
                )
            }

            // Primary Logo (Unified PNG)
            Image(
                painter = logoPainter,
                contentDescription = "Etherion Logo",
                modifier = Modifier.size(160.dp)
            )
        }

        // Terminal-style loading text at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(32.dp)
        ) {
            Text(
                text = "> $bootText",
                color = Color(0xFF00FFC8),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Simulated progress bar
            val progress by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(3000, easing = LinearEasing), label = ""
            )
            
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(2.dp)
                    .background(Color(0xFF004433))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Color(0xFF00FFC8))
                )
            }
        }
    }
}

@Composable
fun BinaryRainBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        repeat(15) {
            Text(
                text = (1..40).map { Random.nextInt(0, 2) }.joinToString(""),
                color = Color(0xFF00FFC8).copy(alpha = alpha),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}
