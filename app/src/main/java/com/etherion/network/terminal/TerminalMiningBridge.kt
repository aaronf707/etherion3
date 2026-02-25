package com.etherion.network.terminal

import com.etherion.network.miner.MiningViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.etherion.network.domain.MiningEquipmentTier
import com.etherion.network.miner.MiningPersistence

class TerminalMiningBridge(
    private val miningViewModel: MiningViewModel,
    private val buffer: TerminalOutputBuffer,
    private val scope: CoroutineScope,
    private val onWatchAd: (onComplete: () -> Unit) -> Unit = {},
    private val onNavigate: (route: String) -> Unit = {}
) {

    fun handle(command: String) {
        val parts = command.trim().lowercase().split(" ")

        when (parts.firstOrNull()) {
            "mine" -> handleMineCommand(parts)
            "wallet" -> {
                buffer.appendSuccess("System: Opening wallet...")
                onNavigate("wallet")
            }
            "stats" -> handleStatsCommand()
            "upgrade" -> handleUpgradeCommand()
            "bright" -> handleBrightCommand(parts)
            "balance" -> handleBalanceCommand()
            "reward" -> handleRewardCommand(parts)
            "ref" -> handleReferralCommand(parts)
            "ads" -> handleAdsCommand(parts)
            "gui" -> handleGuiCommand(parts)
            "settings" -> {
                buffer.appendSuccess("System: Opening settings...")
                onNavigate("settings")
            }
            "top" -> handleTopCommand()
            "tokenomics" -> handleTokenomicsCommand()
            "roadmap" -> handleRoadmapCommand()
            "progress" -> handleProgressCommand()
            "network" -> handleNetworkCommand()
            "news" -> handleNewsCommand()
            "price" -> handlePriceCommand()
            "policy" -> handlePolicyCommand()
            "session" -> handleSessionCommand()
            "renew" -> handleRenewCommand(parts)
            "login" -> buffer.appendSuccess("System: Already authenticated.")
            "logout" -> miningViewModel.logout()
            "sign" -> handleSignCommand(parts)
            "help" -> handleHelpCommand()
            "clear" -> buffer.clear()
            "repair" -> miningViewModel.repairEquipment()
            "share" -> handleShareCommand()
            else -> buffer.appendError("Unknown command: $command. Type 'help'.")
        }
    }

    private fun handleGuiCommand(parts: List<String>) {
        if (parts.size < 2) {
            buffer.appendError("Usage: gui [enable|disable]")
            return
        }
        val persistence = MiningPersistence(miningViewModel.getApplicationContext())
        when (parts[1]) {
            "enable" -> {
                scope.launch {
                    persistence.setGuiModeEnabled(true)
                    buffer.appendSuccess("GUI Mode: ENABLED. Switching to Dashboard...")
                    onNavigate("home")
                }
            }
            "disable" -> {
                scope.launch {
                    persistence.setGuiModeEnabled(false)
                    buffer.appendWarning("GUI Mode: DISABLED. Returning to Terminal...")
                    onNavigate("terminal")
                }
            }
            else -> buffer.appendError("Unknown gui command: ${parts[1]}")
        }
    }

    private fun handleSignCommand(parts: List<String>) {
        if (parts.size < 2) {
            buffer.appendError("Usage: sign out")
            return
        }
        when (parts[1]) {
            "out" -> {
                buffer.appendWarning("System: Initiating logout sequence...")
                miningViewModel.logout()
            }
            "in" -> buffer.appendSuccess("System: Already logged in.")
            else -> buffer.appendError("Unknown sign command: ${parts[1]}")
        }
    }

    private fun handleMineCommand(parts: List<String>) {
        if (parts.size < 2) {
            buffer.appendError("Usage: mine start | mine stop")
            return
        }
        when (parts[1]) {
            "start" -> {
                buffer.appendText("System: Initializing session start ad...")
                onWatchAd {
                    miningViewModel.startMining()
                    buffer.appendSuccess("Mining started.")
                }
            }
            "stop" -> { miningViewModel.stopMining(); buffer.appendSuccess("Mining stopped.") }
            else -> buffer.appendError("Unknown mine command: ${parts[1]}")
        }
    }

    private fun handleStatsCommand() {
        val state = miningViewModel.state.value
        buffer.append(TerminalLine("--- CURRENT NODE PERFORMANCE ---", TerminalLine.LineType.HEADER))
        buffer.appendText("Hashrate: ${"%.2f".format(state.hashrate)} H/s")
        if (state.adBoostActive) {
            buffer.appendSuccess("  [ACTIVE] AD OVERCLOCK: +50%")
        }
        buffer.appendText("Integrity: ${"%.1f".format(state.equipment.integrity * 100)}%")
        buffer.appendText("Equipment: ${state.equipment.tier.displayName}")
        buffer.appendText("Streak Bonus: +${state.streak}%")
        buffer.appendText("Referral Bonus: +${state.teamSize * 2}%")
        buffer.appendText("--------------------------------")
    }

    private fun handleBrightCommand(parts: List<String>) {
        when (parts.getOrNull(1)) {
            "info" -> {
                buffer.appendText(miningViewModel.getBrightPrivacyPolicy())
                buffer.appendWarning("Type 'bright accept' to agree and enable boost.")
            }
            "accept" -> {
                scope.launch {
                    miningViewModel.acceptBrightPolicy()
                    buffer.appendSuccess("Policy Accepted. Bright boost is now available.")
                    buffer.appendText("Type 'bright enable' to start the boost.")
                }
            }
            "enable" -> {
                scope.launch {
                    if (miningViewModel.isBrightPolicyAccepted()) {
                        miningViewModel.setEnabledBright(true)
                        buffer.appendSuccess("Bright Boost: ENABLED. +30% hashrate applied.")
                    } else {
                        buffer.appendError("Error: You must read and accept the policy first.")
                        buffer.appendText("Type 'bright info' to review.")
                    }
                }
            }
            "disable" -> {
                scope.launch {
                    miningViewModel.setEnabledBright(false)
                    buffer.appendWarning("Bright Boost: DISABLED.")
                }
            }
            else -> {
                buffer.appendError("Usage: bright [info|accept|enable|disable]")
            }
        }
    }

    private fun handleTokenomicsCommand() {
        buffer.appendSuccess("--- ETR TOKENOMICS ---")
        buffer.appendText("Total Supply Cap: 21,000,000 ETR")
        buffer.appendText("Current Mined: ~1,420,580 ETR")
        buffer.appendText("Locked/Reserve: 19,429,420 ETR")
        buffer.appendText("Total Burned: 150,000 ETR")
        buffer.appendText("---------------------")
    }

    private fun handleRoadmapCommand() {
        buffer.appendSuccess("--- ETHERION ROADMAP v1 ---")
        buffer.appendText("Q1 2025: Foundation [DONE]")
        buffer.appendText("Q2 2025: Scaling & Rewards [CURRENT]")
        buffer.appendText("Q3 2025: Hardware Utility [PLANNED]")
        buffer.appendText("Q4 2025: Mainnet Bridge [PLANNED]")
        buffer.appendText("---------------------------")
    }

    private fun handleProgressCommand() {
        buffer.appendSuccess("--- DEVELOPMENT PROGRESS ---")
        buffer.appendText("PoW Engine: 100% Complete")
        buffer.appendText("Firebase Identity: 98% Complete")
        buffer.appendText("Monetization Engine: 85% Complete")
        buffer.appendText("Token Migration Protocol: 12% Drafted")
        buffer.appendText("")
        buffer.appendText("ESTIMATED MAINNET LAUNCH: Q4 2025")
        buffer.appendText("----------------------------")
    }

    private fun handlePriceCommand() {
        buffer.appendSuccess("--- PRICE DATA ---")
        buffer.appendText("Launch Target: $1.00 USD / ETR")
        buffer.appendText("Status: Internal value tracking active.")
        buffer.appendText("------------------")
    }

    private fun handlePolicyCommand() {
        buffer.appendSuccess("--- REVENUE POLICY ---")
        buffer.appendText("100% Transparency Rule:")
        buffer.appendText("50% of Ad Revenue is BURNED.")
        buffer.appendText("50% is distributed to Top Miners.")
        buffer.appendText("----------------------")
    }

    private fun handleSessionCommand() {
        val state = miningViewModel.state.value
        val hours = state.sessionTimeRemaining / (1000 * 60 * 60)
        val minutes = (state.sessionTimeRemaining / (1000 * 60)) % 60
        buffer.appendSuccess("--- SESSION INFO ---")
        buffer.appendText("Status: ${if (state.sessionTimeRemaining > 0) "ACTIVE" else "EXPIRED"}")
        buffer.appendText("Time Remaining: %02d:%02d".format(hours, minutes))
        if (state.adBoostActive) {
            val boostMins = state.adBoostTimeRemaining / (1000 * 60)
            buffer.appendSuccess("AD BOOST: ${boostMins}m remaining")
        }
        buffer.appendText("--------------------")
    }

    private fun handleRenewCommand(parts: List<String>) {
        val count = parts.getOrNull(1)?.toIntOrNull() ?: 1
        buffer.appendText("System: Initializing session renewal ad...")
        onWatchAd {
            miningViewModel.renewSession(count)
            buffer.appendSuccess("System: Ad verified. Session extended.")
        }
    }

    private fun handleNetworkCommand() {
        scope.launch {
            buffer.appendText("Syncing with node map...")
            val stats = miningViewModel.getNetworkStats()
            buffer.appendSuccess("--- NETWORK STATS ---")
            buffer.appendText("Total Members: ${stats["totalMembers"]}")
            buffer.appendText("Network Difficulty: 1.42 TH")
            buffer.appendText("---------------------")
        }
    }

    private fun handleNewsCommand() {
        buffer.appendSuccess("--- ETHERION NEWS ---")
        buffer.appendText("[FEB 13] Policy: Ad Rev Buy-back active.")
        buffer.appendText("[FEB 13] ASIC hardware support is LIVE.")
        buffer.appendText("---------------------")
    }

    private fun handleTopCommand() {
        scope.launch {
            buffer.appendText("Fetching top miners...")
            try {
                val db = FirebaseFirestore.getInstance()
                val snap = db.collection("users").orderBy("balance", Query.Direction.DESCENDING).limit(5).get().await()
                buffer.appendSuccess("Current Top 5 Miners:")
                snap.documents.forEachIndexed { i, doc ->
                    buffer.appendText("#${i+1} ${doc.getString("username") ?: "Anon"}: ${"%.2f".format(doc.getDouble("balance") ?: 0.0)} ETR")
                }
            } catch (e: Exception) { buffer.appendError("Error: ${e.message}") }
        }
    }

    private fun handleUpgradeCommand() {
        miningViewModel.upgradeEquipmentLegacy()
        buffer.appendSuccess("Hardware upgrade request processed.")
    }

    private fun handleBalanceCommand() { buffer.appendSuccess("Balance: ${"%.4f".format(miningViewModel.state.value.totalMined)} ETR") }

    private fun handleReferralCommand(parts: List<String>) {
        if (parts.size < 2) { buffer.appendError("Usage: ref code | ref apply <code>"); return }
        if (parts[1] == "code") {
            scope.launch {
                buffer.appendSuccess("Invite Code: ${miningViewModel.getReferralCode()}")
            }
        } else if (parts[1] == "apply" && parts.size > 2) {
            scope.launch {
                val result = miningViewModel.applyReferral(parts[2])
                if (result) buffer.appendSuccess("Referral linked!")
                else buffer.appendError("Already linked or invalid.")
            }
        } else buffer.appendError("Usage: ref apply <code>")
    }

    private fun handleRewardCommand(parts: List<String>) {
        if (parts.getOrNull(1) == "history") {
            scope.launch {
                buffer.appendText("Fetching payout history...")
                val userId = miningViewModel.state.value.userId ?: return@launch
                try {
                    val db = FirebaseFirestore.getInstance()
                    val snap = db.collection("users").document(userId).collection("rewards")
                        .orderBy("timestamp", Query.Direction.DESCENDING).limit(10).get().await()
                    
                    if (snap.isEmpty) {
                        buffer.appendWarning("No reward history found. Keep mining!")
                    } else {
                        buffer.appendSuccess("--- Payout History (Last 10) ---")
                        val df = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
                        snap.documents.forEach { doc ->
                            val type = doc.getString("type") ?: "BONUS"
                            val amount = doc.getDouble("amount") ?: 0.0
                            val date = doc.getLong("timestamp")?.let { df.format(Date(it)) } ?: "Recent"
                            buffer.appendText("[$date] $type: +${"%.4f".format(amount)} ETR")
                        }
                        buffer.appendText("--------------------------------")
                    }
                } catch (e: Exception) { buffer.appendError("Error: ${e.message}") }
            }
        } else {
            miningViewModel.claimDailyReward()
            buffer.appendSuccess("Reward checked.")
            buffer.appendText("Tip: Type 'reward history' to see past payouts.")
        }
    }
    
    private fun handleAdsCommand(parts: List<String>) {
        when (parts.getOrNull(1)) {
            "watch", "start" -> {
                if (miningViewModel.state.value.isAdOpportunityAvailable) {
                    buffer.appendText("System: Initializing ad stream...")
                    onWatchAd {
                        miningViewModel.applyAdReward()
                    }
                } else {
                    buffer.appendError("No ad opportunity available right now.")
                }
            }
            "stats" -> {
                scope.launch {
                    val userId = miningViewModel.state.value.userId ?: return@launch
                    val db = FirebaseFirestore.getInstance()
                    val snap = db.collection("users").document(userId).collection("rewards")
                        .whereEqualTo("type", "AD_REWARD").get().await()
                    val totalAds = snap.size()
                    val totalEarnings = snap.documents.sumOf { it.getDouble("amount") ?: 0.0 }
                    buffer.appendSuccess("--- Ad Reward Stats ---")
                    buffer.appendText("Total Ads Watched: $totalAds")
                    buffer.appendText("Total Ad Earnings: ${"%.4f".format(totalEarnings)} ETR")
                    buffer.appendText("------------------------")
                }
            }
            else -> {
                buffer.appendWarning("Ad opportunity: Watch for +0.05 ETR boost.")
                buffer.appendText("Commands: ads watch, ads stats")
            }
        }
    }

    private fun handleShareCommand() {
        scope.launch {
            val code = miningViewModel.getReferralCode()
            val context = miningViewModel.getApplicationContext()
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Etherion Mining Team")
                putExtra(android.content.Intent.EXTRA_TEXT, "Join my mining team on Etherion! Use my code $code to get a 1.00 ETR signup bonus. Download now and start earning!")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share your invite code").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            buffer.appendSuccess("System: Initializing sharing protocol...")
        }
    }

    private fun handleHelpCommand() {
        buffer.appendSuccess("Full Command Directory:")
        buffer.appendText("BASIC: mine [start|stop], stats, balance, wallet, repair")
        buffer.appendText("CRYPTO: tokenomics, price, policy, top, network")
        buffer.appendText("SOCIAL: news, roadmap, progress, ref [code|apply], reward history, share")
        buffer.appendText("MINING: session, renew, upgrade, ads watch, ads stats")
        buffer.appendText("SYSTEM: gui [enable|disable], settings, login, sign out, help")
    }
}
