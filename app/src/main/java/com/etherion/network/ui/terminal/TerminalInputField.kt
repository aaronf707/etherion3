package com.etherion.network.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun TerminalInputField(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    // Neon green box around the input box and run button
    Box(
        modifier = modifier
            .padding(8.dp)
            .border(width = 2.dp, color = Color(0xFF00FF00), shape = RoundedCornerShape(4.dp))
            .background(Color.Black, shape = RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Enter command...", color = Color(0xFF004400), fontFamily = FontFamily.Monospace) },
                textStyle = LocalTextStyle.current.copy(
                    color = Color(0xFF00FF00),
                    fontFamily = FontFamily.Monospace
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Black,
                    unfocusedContainerColor = Color.Black,
                    cursorColor = Color(0xFF00FF00),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSubmit(text)
                        text = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF00),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("RUN", fontFamily = FontFamily.Monospace)
            }
        }
    }
}
