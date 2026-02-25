package com.etherion.network.terminal

import kotlinx.coroutines.delay

class TerminalCommandEngine(
    private val buffer: TerminalOutputBuffer
) {

    suspend fun execute(command: String) {
        val trimmed = command.trim()

        if (trimmed.isEmpty()) return

        buffer.append(TerminalLine("> $trimmed", TerminalLine.LineType.INPUT))

        when (trimmed.lowercase()) {

            "help" -> showHelp()

            "clear" -> buffer.clear()

            "status" -> {
                buffer.appendSuccess("System Status: OK")
                buffer.appendText("Uptime: Active")
                buffer.appendText("Mining Engine: Ready")
                buffer.appendText("Network: Connected")
            }

            "about" -> {
                buffer.appendText("Etherion Network Terminal")
                buffer.appendText("Version 1.0")
                buffer.appendText("© Etherion Labs")
            }

            "mine" -> { /* Handled by bridge, but listed here for help */ }
            "stats" -> { /* Handled by bridge */ }
            "wallet" -> { /* Handled by bridge */ }
            "balance" -> { /* Handled by bridge */ }
            "reward" -> { /* Handled by bridge */ }
            "ref" -> { /* Handled by bridge */ }

            else -> {
                // If not a system command, it might be a mining command handled by bridge.
                // The bridge call in MiningTerminalScreen handles the execution.
                // We don't append error here yet because the bridge is the primary router now.
            }
        }
    }

    private fun showHelp() {
        buffer.appendSuccess("Available Commands:")
        buffer.appendText("help    - Show this help menu")
        buffer.appendText("mine    - mine start | mine stop")
        buffer.appendText("stats   - Show mining statistics")
        buffer.appendText("wallet  - Show your ETR address")
        buffer.appendText("balance - Show your current tokens")
        buffer.appendText("reward  - Claim daily ETR reward")
        buffer.appendText("ref     - ref code | ref apply <code>")
        buffer.appendText("status  - Show system status")
        buffer.appendText("clear   - Clear terminal output")
        buffer.appendText("about   - About Etherion")
    }
}
