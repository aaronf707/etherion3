package com.etherion.network.terminal

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.NORMAL,
    val action: TerminalAction? = null,
    val id: String = java.util.UUID.randomUUID().toString()
) {
    enum class LineType {
        NORMAL,
        SUCCESS,
        ERROR,
        WARNING,
        INPUT,
        LINK,
        DISABLED_LINK,
        HEADER,
        COMMAND_DETAIL
    }

    enum class TerminalAction {
        WATCH_AD,
        SOLVE_BLOCK
    }
}
