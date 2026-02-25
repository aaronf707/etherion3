package com.etherion.network.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.etherion.network.terminal.TerminalLine

@Composable
fun TerminalWindow(
    lines: List<TerminalLine>,
    onActionClick: (TerminalLine) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .padding(8.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(lines, key = { it.id }) { line ->
                val isClickable = line.action != null && line.type == TerminalLine.LineType.LINK
                
                val color = when (line.type) {
                    TerminalLine.LineType.SUCCESS -> Color(0xFF00FF00)
                    TerminalLine.LineType.ERROR -> Color(0xFFFF3333)
                    TerminalLine.LineType.WARNING -> Color(0xFFFFCC00)
                    TerminalLine.LineType.INPUT -> Color(0xFF00AAFF)
                    TerminalLine.LineType.LINK -> Color(0xFF00FFFF)
                    TerminalLine.LineType.DISABLED_LINK -> Color(0xFF555555)
                    TerminalLine.LineType.HEADER -> Color(0xFFFFFFFF) // White for category headers
                    TerminalLine.LineType.COMMAND_DETAIL -> Color(0xFF00FFFF) // Cyan for commands
                    else -> Color(0xFF00CC00) // Default green
                }

                val fontWeight = if (line.type == TerminalLine.LineType.HEADER) FontWeight.Bold else FontWeight.Normal
                val fontSize = if (line.type == TerminalLine.LineType.HEADER) 13.sp else 12.sp

                Text(
                    text = line.text,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = fontWeight,
                    fontSize = fontSize,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (isClickable) TextDecoration.Underline else TextDecoration.None,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isClickable) Modifier.clickable { onActionClick(line) } else Modifier)
                        .padding(vertical = 1.dp)
                )
            }
        }
    }
}
