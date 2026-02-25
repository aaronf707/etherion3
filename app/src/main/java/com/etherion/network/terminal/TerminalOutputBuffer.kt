package com.etherion.network.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TerminalOutputBuffer(
    private val maxLines: Int = 200
) {
    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines

    fun append(line: TerminalLine) {
        val updated = (_lines.value + line).takeLast(maxLines)
        _lines.value = updated
    }

    fun updateLine(index: Int, transform: (TerminalLine) -> TerminalLine) {
        val current = _lines.value.toMutableList()
        if (index in current.indices) {
            current[index] = transform(current[index])
            _lines.value = current
        }
    }

    fun appendText(text: String) {
        append(TerminalLine(text))
    }

    fun appendSuccess(text: String) {
        append(TerminalLine(text, TerminalLine.LineType.SUCCESS))
    }

    fun appendError(text: String) {
        append(TerminalLine(text, TerminalLine.LineType.ERROR))
    }

    fun appendWarning(text: String) {
        append(TerminalLine(text, TerminalLine.LineType.WARNING))
    }

    fun clear() {
        _lines.value = emptyList()
    }
}
