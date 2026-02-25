package com.etherion.network.miner

import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.etherion.network.auth.AuthManager
import com.etherion.network.domain.MiningEconomyManager
import com.etherion.network.domain.MiningEquipment
import com.etherion.network.domain.BrightManager
import com.etherion.network.referrals.ReferralManager
import com.etherion.network.rewards.RewardManager
import com.etherion.network.sync.SyncManager
import com.etherion.network.terminal.TerminalLine
import com.etherion.network.terminal.TerminalOutputBuffer
import com.etherion.network.wallet.WalletManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.ListenerRegistration
import com.etherion.network.domain.MiningEquipmentTier
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MiningViewModel(
    private val appContext: Context,
    private val economy: MiningEconomyManager = MiningEconomyManager(),
    private val bright: BrightManager = BrightManager(appContext)
) : ViewModel() {

    private val walletManager = WalletManager(appContext)
    private val persistence = MiningPersistence(appContext)
    private val rewards = RewardManager(appContext)
    private val referrals = ReferralManager(appContext)
    private val auth = AuthManager()
    private val sync = SyncManager()

    val commandTerminalBuffer = TerminalOutputBuffer()
    val miningTerminalBuffer = TerminalOutputBuffer()

    data class MiningState(
        val isMining: Boolean = false,
        val hashrate: Double = 0.0,
        val totalMined: Double = 0.0, // This is the Wallet Balance (Lifetime Ledger)
        val sessionEarnings: Double = 0.0, // This is the Session Ticker
        val equipment: MiningEquipment = MiningEquipment(),
        val status: String = "Initializing...",
        val userId: String? = null,
        val username: String = "GUEST MINER",
        val streak: Int = 0,
        val teamSize: Int = 0,
        val isBrightActive: Boolean = false,
        val canClaimDailyReward: Boolean = false,
        val totalInvestment: Double = 0.0,
        val sessionTimeRemaining: Long = 0L,
        val adBoostActive: Boolean = false,
        val adBoostTimeRemaining: Long = 0L,
        val userLevel: Int = 1,
        val userExperience: Long = 0L,
        val totalDownloads: Long = 0,
        val activeMiners: Long = 0,
        val projectedTokenValue: Double = 0.0,
        val tickerMessage: String = "ESTABLISHING SECURE NODE...",
        val adRegion: String = "TIER_3",
        val regionalMultiplier: Double = 0.3,
        val isAdOpportunityAvailable: Boolean = false,
        val isBlockSolveAvailable: Boolean = false,
        val isDataSdkOptedIn: Boolean = false,
        val nodeVersion: String = "1.0.4-beta"
    )

    private val _state = MutableStateFlow(MiningState())
    val state: StateFlow<MiningState> = _state

    private val SESSION_DURATION = 4 * 60 * 60 * 1000L
    private val AD_BOOST_DURATION = 1 * 60 * 60 * 1000L
    private var miningService: MiningService? = null
    private var isBound = false
    private var tickerListener: ListenerRegistration? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MiningService.LocalBinder
            miningService = binder.getService()
            isBound = true
            
            viewModelScope.launch {
                miningService?.serviceState?.collect { serviceState ->
                    val oldMining = _state.value.isMining
                    _state.value = _state.value.copy(
                        isMining = serviceState.isMining,
                        hashrate = serviceState.hashrate,
                        totalMined = serviceState.totalMined,
                        sessionEarnings = serviceState.sessionEarnings,
                        equipment = serviceState.equipment,
                        status = serviceState.status,
                        teamSize = serviceState.teamSize,
                        isBrightActive = serviceState.isBrightActive,
                        sessionTimeRemaining = serviceState.sessionTimeRemaining,
                        adBoostActive = serviceState.adBoostActive,
                        adBoostTimeRemaining = serviceState.adBoostTimeRemaining,
                        adRegion = serviceState.adRegion,
                        regionalMultiplier = serviceState.regionalMultiplier,
                        isAdOpportunityAvailable = serviceState.isAdOpportunityAvailable,
                        isBlockSolveAvailable = serviceState.isBlockSolveAvailable,
                        isDataSdkOptedIn = serviceState.isDataSdkOptedIn
                    )
                    updateLevelLogic()
                    
                    if (oldMining != serviceState.isMining) {
                        syncAllDataToFirebase()
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            miningService = null
        }
    }

    private fun updateLevelLogic() {
        val balance = _state.value.totalMined
        val newLevel = (kotlin.math.sqrt(balance / 5.0).toInt() + 1).coerceAtLeast(1)
        if (newLevel != _state.value.userLevel) {
            _state.value = _state.value.copy(userLevel = newLevel)
        }
    }

    init {
        _state.value = _state.value.copy(totalMined = persistence.loadBalance())
        
        Intent(appContext, MiningService::class.java).also { intent ->
            appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        viewModelScope.launch {
            auth.userFlow.collect { user ->
                if (user != null) {
                    updateUserDisplay(user)
                    authenticateAndSync()
                } else {
                    _state.value = _state.value.copy(username = "GUEST MINER", userId = null)
                }
            }
        }

        viewModelScope.launch {
            rewards.canClaimToday().collect { canClaim ->
                _state.value = _state.value.copy(canClaimDailyReward = canClaim)
            }
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isBrightActive = bright.isEnabled.first())
        }
        
        setupRealtimeTicker()
        startGlobalStatsTicker()
    }

    private fun setupRealtimeTicker() {
        val db = FirebaseFirestore.getInstance()
        tickerListener = db.collection("market").document("ticker")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MiningVM", "Ticker listen failed", e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val remoteMessage = snapshot.getString("message")
                    if (remoteMessage != null) {
                        _state.value = _state.value.copy(tickerMessage = remoteMessage)
                    }
                }
            }
    }

    private fun startGlobalStatsTicker() {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()
            while (true) {
                try {
                    // 1. Fetch count of total registered users
                    val totalUsersCount = db.collection("users").count()
                        .get(AggregateSource.SERVER).await().count
                    
                    // 2. Fetch count of users currently set to isMining = true
                    // We removed the 'lastActive' filter temporarily to avoid Index errors
                    val activeCount = db.collection("users")
                        .whereEqualTo("isMining", true)
                        .count().get(AggregateSource.SERVER).await().count

                    // 3. Get market totals for token value
                    val marketSnap = db.collection("market").document("stats").get().await()
                    val totalMinedETR = marketSnap.getDouble("totalETRMined") ?: 1.0
                    
                    val totalRevenue = economy.calculateProjectedTotalRevenue(
                        activeUsers = maxOf(1, activeCount), 
                        avgRewardedAdsPerUser = 2.5,
                        dataSdkOptInRate = 0.4
                    )
                    
                    val userBudgetUSD = totalRevenue * 0.75
                    val calculatedVal = userBudgetUSD / maxOf(1.0, totalMinedETR)

                    // Update UI state with actual data
                    _state.value = _state.value.copy(
                        totalDownloads = totalUsersCount,
                        activeMiners = activeCount,
                        projectedTokenValue = calculatedVal
                    )
                } catch (e: Exception) {
                    Log.e("MiningVM", "Stats update failed: ${e.message}")
                    // Basic fallback to keep UI alive
                    val currentActive = if (_state.value.isMining) 1L else 0L
                    _state.value = _state.value.copy(activeMiners = currentActive)
                }
                delay(20000) // Refresh every 20 seconds
            }
        }
    }

    private suspend fun updateUserDisplay(user: FirebaseUser) {
        if (user.displayName == null) {
            try { user.reload().await() } catch (e: Exception) { Log.e("MiningVM", "User reload failed", e) }
        }
        val rawName = user.displayName ?: user.email?.substringBefore("@") ?: "USER"
        val formattedName = rawName.uppercase()
        _state.value = _state.value.copy(userId = user.uid, username = formattedName)
    }

    private fun authenticateAndSync() {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val cloudData = try { sync.downloadUserData(user.uid) } catch (e: Exception) { null }

            if (cloudData != null) {
                val lastActive = cloudData["lastActive"] as? Long ?: System.currentTimeMillis()
                val savedSessionEndTime = cloudData["sessionEndTime"] as? Long ?: 0L
                val currentTime = System.currentTimeMillis()
                
                var currentBalance = cloudData["balance"] as? Double ?: 0.0
                var sessionRemaining = savedSessionEndTime - currentTime
                val savedUsername = cloudData["username"] as? String
                val investment = cloudData["totalInvestment"] as? Double ?: 0.0
                val savedSessionEarnings = cloudData["sessionEarnings"] as? Double ?: 0.0
                val savedLevel = (cloudData["userLevel"] as? Long)?.toInt() ?: 1
                val savedAdRegion = cloudData["adRegion"] as? String ?: "TIER_3"
                val dataSdkOptIn = cloudData["dataSdkOptIn"] as? Boolean ?: false
                val regionalMult = economy.getRegionalMultiplier(savedAdRegion)

                if (sessionRemaining > -SESSION_DURATION && savedSessionEndTime > lastActive) {
                    val timePassedWhileOffline = minOf(currentTime, savedSessionEndTime) - lastActive
                    if (timePassedWhileOffline > 0) {
                        val hashrate = economy.calculateHashrate(
                            equipment = _state.value.equipment, 
                            streakDays = _state.value.streak,
                            regionalMultiplier = regionalMult,
                            dataSdkBoost = dataSdkOptIn
                        )
                        val offlineEarnings = (timePassedWhileOffline / 1000.0) * (economy.calculateEarnings(hashrate) / 2.0)
                        currentBalance += offlineEarnings
                    }
                }

                _state.value = _state.value.copy(
                    totalMined = currentBalance,
                    sessionTimeRemaining = maxOf(0L, sessionRemaining),
                    status = "Node Synced",
                    username = if (savedUsername?.contains("GUEST") == true) _state.value.username else (savedUsername ?: _state.value.username).uppercase(),
                    totalInvestment = investment,
                    sessionEarnings = savedSessionEarnings,
                    userLevel = savedLevel,
                    adRegion = savedAdRegion,
                    regionalMultiplier = regionalMult,
                    isDataSdkOptedIn = dataSdkOptIn
                )
                miningService?.updateState(_state.value)
                
                if (sessionRemaining > 0) startMining(isAutoResume = true)
            }
            startSyncLoop(user.uid)
        }
    }

    private fun startSyncLoop(userId: String) {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                syncAllDataToFirebase()
            }
        }
    }

    fun syncAllDataToFirebase() {
        val user = auth.currentUser ?: return
        val currentState = _state.value
        val sessionEndTime = System.currentTimeMillis() + currentState.sessionTimeRemaining
        
        val data = mapOf(
            "balance" to currentState.totalMined,
            "username" to currentState.username,
            "hashrate" to currentState.hashrate,
            "equipmentTier" to currentState.equipment.tier.name,
            "streak" to currentState.streak,
            "teamSize" to currentState.teamSize,
            "totalInvestment" to currentState.totalInvestment,
            "sessionEndTime" to sessionEndTime,
            "lastActive" to System.currentTimeMillis(),
            "isMining" to currentState.isMining,
            "isBrightActive" to currentState.isBrightActive,
            "nodeVersion" to "1.1.0",
            "adBoostActive" to currentState.adBoostActive,
            "adBoostTimeRemaining" to currentState.adBoostTimeRemaining,
            "sessionEarnings" to currentState.sessionEarnings,
            "userLevel" to currentState.userLevel,
            "adRegion" to currentState.adRegion,
            "dataSdkOptIn" to currentState.isDataSdkOptedIn
        )

        viewModelScope.launch {
            sync.uploadUserData(user.uid, data)
        }
    }

    fun startMining(isAutoResume: Boolean = false) {
        if (!isAutoResume) {
            _state.value = _state.value.copy(
                sessionTimeRemaining = SESSION_DURATION,
                sessionEarnings = 0.0
            )
        }

        if (_state.value.sessionTimeRemaining <= 0) return
        
        miningService?.updateState(_state.value)
        val intent = Intent(appContext, MiningService::class.java).apply { action = "START" }
        appContext.startForegroundService(intent)
        
        syncAllDataToFirebase()
    }

    fun stopMining() {
        val intent = Intent(appContext, MiningService::class.java).apply { action = "STOP" }
        appContext.startService(intent)
        
        _state.value = _state.value.copy(isMining = false)
        syncAllDataToFirebase()
    }

    fun logout() {
        stopMining()
        _state.value = MiningState(status = "Logging Out...")
        viewModelScope.launch {
            auth.signOut(appContext)
            _state.value = MiningState(status = "Logged Out")
        }
    }

    private fun recordTransaction(type: String, amount: Double) {
        val userId = auth.getUserId() ?: return
        if (amount <= 0) return
        
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()
            val tx = mapOf(
                "type" to type,
                "amount" to amount,
                "timestamp" to System.currentTimeMillis()
            )
            try {
                db.collection("users").document(userId).collection("rewards").add(tx).await()
            } catch (e: Exception) {
                Log.e("MiningVM", "Failed to record transaction", e)
            }
        }
    }

    fun claimDailyReward(): Double {
        val result = rewards.claimDaily()
        val amount = if (result.message == "Already claimed today!") 0.0 else result.reward * 0.01
        if (amount > 0) {
            val newTotal = _state.value.totalMined + amount
            persistence.saveBalance(newTotal)
            _state.value = _state.value.copy(totalMined = newTotal, streak = result.streak)
            miningService?.updateState(_state.value)
            
            recordTransaction("DAILY_REWARD", amount)
            syncAllDataToFirebase()
        }
        return amount
    }

    fun claimAdBonus() {
        val bonus = economy.getAdTokenBoost()
        val newTotal = _state.value.totalMined + bonus
        persistence.saveBalance(newTotal)
        _state.value = _state.value.copy(totalMined = newTotal)
        miningService?.updateState(_state.value)
        
        recordTransaction("AD_REWARD", bonus)
        syncAllDataToFirebase()
    }

    fun upgradeEquipment(targetTier: MiningEquipmentTier) {
        val currentBalance = _state.value.totalMined
        val cost = targetTier.cost
        
        if (currentBalance >= cost) {
            val newBalance = currentBalance - cost
            val newInvestment = _state.value.totalInvestment + cost
            persistence.saveBalance(newBalance)
            
            val upgraded = economy.upgradeEquipment(_state.value.equipment, targetTier)
            val newState = _state.value.copy(
                totalMined = newBalance,
                equipment = upgraded,
                totalInvestment = newInvestment
            )
            _state.value = newState
            miningService?.updateState(newState)
            
            recordTransaction("HARDWARE_UPGRADE", -cost)
            syncAllDataToFirebase()
        }
    }

    fun repairEquipment() {
        val cost = 0.5
        if (_state.value.totalMined >= cost) {
            val newTotal = _state.value.totalMined - cost
            persistence.saveBalance(newTotal)
            val updatedEquipment = _state.value.equipment.copy(integrity = 1.0)
            val newState = _state.value.copy(totalMined = newTotal, equipment = updatedEquipment)
            _state.value = newState
            miningService?.updateState(newState)
            
            recordTransaction("REPAIR_GEAR", -cost)
            syncAllDataToFirebase()
        }
    }

    suspend fun getReferralCode(): String = referrals.getOrCreateReferralCode(_state.value.userId ?: "")
    
    suspend fun updateUsername(newName: String): Boolean {
        if (newName.isBlank() || newName.length < 3 || newName.length > 15) return false
        val success = auth.updateDisplayName(newName)
        if (success) {
            _state.value = _state.value.copy(username = newName.uppercase())
            syncAllDataToFirebase()
        }
        return success
    }

    fun getApplicationContext(): Context = appContext
    fun isBrightEnabled(): Boolean = runBlocking { bright.isEnabled.first() }
    fun isBrightPolicyAccepted(): Boolean = runBlocking { bright.isPolicyAccepted.first() }
    fun getBrightPrivacyPolicy(): String = bright.getPrivacyText()
    suspend fun acceptBrightPolicy() = bright.acceptPolicy()
    suspend fun setEnabledBright(enabled: Boolean) {
        bright.setEnabled(enabled)
        _state.value = _state.value.copy(isBrightActive = enabled)
        miningService?.updateState(_state.value)
    }
    fun renewSession(count: Int = 1) {
        val addedTime = count * SESSION_DURATION
        val newTime = _state.value.sessionTimeRemaining + addedTime
        _state.value = _state.value.copy(sessionTimeRemaining = newTime)
        miningService?.updateState(_state.value)
        syncAllDataToFirebase()
    }
    suspend fun getNetworkStats(): Map<String, Long> {
        val count = try { FirebaseFirestore.getInstance().collection("users").count().get(AggregateSource.SERVER).await().count } catch(e: Exception) { 0L }
        return mapOf("totalMembers" to count)
    }
    fun upgradeEquipmentLegacy() {
        val nextTier = when (_state.value.equipment.tier) {
            MiningEquipmentTier.CPU -> MiningEquipmentTier.GPU_ENTRY
            MiningEquipmentTier.GPU_ENTRY -> MiningEquipmentTier.GPU_PRO
            MiningEquipmentTier.GPU_PRO -> MiningEquipmentTier.RIG_SMALL
            MiningEquipmentTier.RIG_SMALL -> MiningEquipmentTier.ASIC_STANDARD
            MiningEquipmentTier.ASIC_STANDARD -> MiningEquipmentTier.ASIC_PRO
            MiningEquipmentTier.ASIC_PRO -> MiningEquipmentTier.ASIC_PRO
        }
        upgradeEquipment(nextTier)
    }
    fun applyReferral(code: String): Boolean = runBlocking { referrals.applyReferrer(_state.value.userId ?: "", code).first }
    fun applyAdReward() {
        val newTime = AD_BOOST_DURATION
        val newState = _state.value.copy(adBoostActive = true, adBoostTimeRemaining = newTime)
        _state.value = newState
        miningService?.updateState(newState)
        syncAllDataToFirebase()
    }
    fun solveBlockReward() {
        val reward = 0.25
        val newTotal = _state.value.totalMined + reward
        val newState = _state.value.copy(totalMined = newTotal)
        _state.value = newState
        miningService?.updateState(newState)
        
        recordTransaction("BLOCK_SOLVED", reward)
        syncAllDataToFirebase()
    }

    override fun onCleared() { 
        if (isBound) { appContext.unbindService(serviceConnection); isBound = false }
        tickerListener?.remove()
        super.onCleared() 
    }
}
