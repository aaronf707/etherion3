package com.etherion.network.miner

import android.content.*
import android.net.Uri
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
import com.etherion.network.sync.ImageStorageManager
import com.etherion.network.terminal.TerminalLine
import com.etherion.network.terminal.TerminalOutputBuffer
import com.etherion.network.wallet.WalletManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.ListenerRegistration
import com.etherion.network.domain.MiningEquipmentTier
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
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
    private val storage = ImageStorageManager()

    val commandTerminalBuffer = TerminalOutputBuffer()
    val miningTerminalBuffer = TerminalOutputBuffer()

    data class TeamMember(
        val username: String,
        val level: Int,
        val isMining: Boolean
    )

    data class Notification(
        val id: String = "",
        val title: String = "",
        val message: String = "",
        val timestamp: Long = 0L,
        val isRead: Boolean = false,
        val type: String = "GENERAL"
    )

    data class MiningState(
        val isMining: Boolean = false,
        val hashrate: Double = 0.0,
        val totalMined: Double = 0.0, 
        val sessionEarnings: Double = 0.0, 
        val equipment: MiningEquipment = MiningEquipment(),
        val status: String = "Initializing...",
        val userId: String? = null,
        val username: String = "GUEST MINER",
        val profilePictureUrl: String? = null,
        val ownedAvatars: List<String> = listOf("Person", "Shield", "Hub", "Security", "Terminal"),
        val streak: Int = 0,
        val teamSize: Int = 0,
        val teamMembers: List<TeamMember> = emptyList(),
        val notifications: List<Notification> = emptyList(),
        val unreadNotificationCount: Int = 0,
        val referrerUid: String? = null,
        val referrerCode: String? = null,
        val myInviteCode: String = "...",
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
        val nodeVersion: String = "1.0.4-beta",
        val maxHashrate: Double = 0.0,
        val joinedTimestamp: Long = 0L,
        val lifetimeMined: Double = 0.0,
        val rank: String = "UNRANKED"
    )

    private val _state = MutableStateFlow(MiningState())
    val state: StateFlow<MiningState> = _state

    private val SESSION_DURATION = 4 * 60 * 60 * 1000L
    private val AD_BOOST_DURATION = 1 * 60 * 60 * 1000L
    private var miningService: MiningService? = null
    private var isBound = false
    private var tickerListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MiningService.LocalBinder
            miningService = binder.getService()
            isBound = true
            
            viewModelScope.launch {
                miningService?.serviceState?.collect { serviceState ->
                    val currentMaxHashrate = maxOf(_state.value.maxHashrate, serviceState.hashrate)
                    
                    // CRITICAL FIX: Only copy SESSION data from service. 
                    // Do NOT copy identity data (teamSize, username) which should come from ViewModel/Cloud.
                    _state.value = _state.value.copy(
                        isMining = serviceState.isMining,
                        hashrate = serviceState.hashrate,
                        totalMined = serviceState.totalMined,
                        sessionEarnings = serviceState.sessionEarnings,
                        equipment = serviceState.equipment,
                        status = serviceState.status,
                        isBrightActive = serviceState.isBrightActive,
                        sessionTimeRemaining = serviceState.sessionTimeRemaining,
                        adBoostActive = serviceState.adBoostActive,
                        adBoostTimeRemaining = serviceState.adBoostTimeRemaining,
                        isAdOpportunityAvailable = serviceState.isAdOpportunityAvailable,
                        isBlockSolveAvailable = serviceState.isBlockSolveAvailable,
                        isDataSdkOptedIn = serviceState.isDataSdkOptedIn,
                        maxHashrate = currentMaxHashrate
                    )
                    updateLevelLogic()
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
                    authenticateAndSync(user)
                    syncReferralAndTeamData(user.uid)
                    setupNotificationListener(user.uid)
                    _state.value = _state.value.copy(myInviteCode = referrals.getOrCreateReferralCode(user.uid))
                } else {
                    _state.value = _state.value.copy(username = "GUEST MINER", userId = null, profilePictureUrl = null)
                    notificationListener?.remove()
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

    private fun setupNotificationListener(userId: String) {
        notificationListener?.remove()
        val db = FirebaseFirestore.getInstance()
        notificationListener = db.collection("users").document(userId).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val notifs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    }
                    val unread = notifs.count { !it.isRead }
                    _state.value = _state.value.copy(notifications = notifs, unreadNotificationCount = unread)
                }
            }
    }

    fun markNotificationsAsRead() {
        val userId = _state.value.userId ?: return
        val db = FirebaseFirestore.getInstance()
        viewModelScope.launch {
            val unread = _state.value.notifications.filter { !it.isRead }
            if (unread.isEmpty()) return@launch
            
            val batch = db.batch()
            unread.forEach { notif ->
                val ref = db.collection("users").document(userId).collection("notifications").document(notif.id)
                batch.update(ref, "isRead", true)
            }
            try { batch.commit().await() } catch (e: Exception) { Log.e("MiningVM", "Failed to mark as read", e) }
        }
    }

    private suspend fun syncReferralAndTeamData(userId: String) {
        val db = FirebaseFirestore.getInstance()
        try {
            // 1. Sync Referrer Info
            val userDoc = db.collection("users").document(userId).get().await()
            val referrerUid = userDoc.getString("referrerUid")
            if (referrerUid != null) {
                val referralDoc = db.collection("referrals").document(userId).get().await()
                if (referralDoc.exists()) {
                    _state.value = _state.value.copy(
                        referrerUid = referrerUid,
                        referrerCode = referralDoc.getString("code") ?: "NODE_UPLINK"
                    )
                }
            }

            // 2. Fetch Team Members and recalculate teamSize
            val teamDocs = db.collection("referrals")
                .whereEqualTo("referrerUid", userId)
                .get().await()
            
            val currentTeamSize = teamDocs.size()
            val members = mutableListOf<TeamMember>()
            
            for (doc in teamDocs.documents) {
                val memberUid = doc.getString("refereeUid") ?: continue
                val memberUserDoc = db.collection("users").document(memberUid).get().await()
                if (memberUserDoc.exists()) {
                    members.add(TeamMember(
                        username = memberUserDoc.getString("username") ?: "MINER_${memberUid.take(4)}",
                        level = (memberUserDoc.getLong("userLevel") ?: 1L).toInt(),
                        isMining = memberUserDoc.getBoolean("isMining") ?: false
                    ))
                }
            }

            // 3. Update local state and Firestore if teamSize changed
            if (currentTeamSize != _state.value.teamSize) {
                db.collection("users").document(userId).update("teamSize", currentTeamSize).await()
            }
            
            val updatedState = _state.value.copy(
                teamSize = currentTeamSize,
                teamMembers = members
            )
            _state.value = updatedState
            
            // Sync the NEW team size with the background service immediately
            miningService?.updateState(updatedState)

            // 4. Check for dividends (Mining Kickbacks)
            checkForUnclaimedReferralRewards(userId)

        } catch (e: Exception) {
            Log.e("MiningVM", "Referral sync failed", e)
        }
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
                    val totalUsersCount = db.collection("users").count()
                        .get(AggregateSource.SERVER).await().count
                    
                    val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
                    val activeCount = db.collection("users")
                        .whereEqualTo("isMining", true)
                        .whereGreaterThan("lastActive", fiveMinutesAgo)
                        .count().get(AggregateSource.SERVER).await().count

                    val marketSnap = db.collection("market").document("stats").get().await()
                    val totalMinedETR = marketSnap.getDouble("totalETRMined") ?: 1.0
                    
                    val totalRevenue = economy.calculateProjectedTotalRevenue(
                        activeUsers = maxOf(1, activeCount), 
                        avgRewardedAdsPerUser = 2.5,
                        dataSdkOptInRate = 0.4
                    )
                    
                    val userBudgetUSD = totalRevenue * 0.75
                    val calculatedVal = userBudgetUSD / maxOf(1.0, totalMinedETR)

                    _state.value = _state.value.copy(
                        totalDownloads = totalUsersCount,
                        activeMiners = activeCount,
                        projectedTokenValue = calculatedVal
                    )
                    
                    updateUserRank()
                } catch (e: Exception) {
                    val currentActive = if (_state.value.isMining) 1L else 0L
                    _state.value = _state.value.copy(activeMiners = currentActive)
                }
                delay(20000) 
            }
        }
    }

    private suspend fun updateUserRank() {
        val userId = _state.value.userId ?: return
        val db = FirebaseFirestore.getInstance()
        try {
            val userBalance = _state.value.totalMined
            val betterThanMe = db.collection("users")
                .whereGreaterThan("balance", userBalance)
                .count().get(AggregateSource.SERVER).await().count
            
            val currentRank = betterThanMe + 1
            val rankTitle = when {
                currentRank == 1L -> "GENESIS NODE"
                currentRank <= 10L -> "ELITE NODE"
                currentRank <= 100L -> "PRO NODE"
                else -> "STANDARD NODE"
            }
            _state.value = _state.value.copy(rank = rankTitle)
        } catch (e: Exception) {
            Log.e("MiningVM", "Rank update failed", e)
        }
    }

    private suspend fun updateUserDisplay(user: FirebaseUser) {
        if (user.displayName == null) {
            try { user.reload().await() } catch (e: Exception) { Log.e("MiningVM", "User reload failed", e) }
        }
        val rawName = user.displayName ?: user.email?.substringBefore("@") ?: "USER"
        val formattedName = rawName.uppercase()
        _state.value = _state.value.copy(
            userId = user.uid,
            username = formattedName,
            profilePictureUrl = user.photoUrl?.toString()
        )
    }

    private suspend fun authenticateAndSync(user: FirebaseUser): Map<String, Any>? {
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
            
            val joinedTs = cloudData["joinedTimestamp"] as? Long ?: user.metadata?.creationTimestamp ?: System.currentTimeMillis()
            val lifetimeMined = cloudData["lifetimeMined"] as? Double ?: currentBalance
            val maxHash = cloudData["maxHashrate"] as? Double ?: 0.0
            val savedPhotoUrl = cloudData["profilePictureUrl"] as? String
            
            val rawOwnedAvatars = cloudData["ownedAvatars"]
            val ownedAvatars = if (rawOwnedAvatars is List<*>) {
                rawOwnedAvatars.filterIsInstance<String>()
            } else {
                listOf("Person", "Shield", "Hub", "Security", "Terminal")
            }
            
            val savedTeamSize = (cloudData["teamSize"] as? Long)?.toInt() ?: 0
            val savedReferrer = cloudData["referrerUid"] as? String

            if (sessionRemaining > -SESSION_DURATION && savedSessionEndTime > lastActive) {
                val timePassedWhileOffline = minOf(currentTime, savedSessionEndTime) - lastActive
                if (timePassedWhileOffline > 0) {
                    val hashrate = economy.calculateHashrate(
                        equipment = _state.value.equipment, 
                        streakDays = _state.value.streak,
                        teamSize = savedTeamSize,
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
                isDataSdkOptedIn = dataSdkOptIn,
                joinedTimestamp = joinedTs,
                lifetimeMined = maxOf(lifetimeMined, currentBalance),
                maxHashrate = maxHash,
                profilePictureUrl = user.photoUrl?.toString() ?: savedPhotoUrl,
                ownedAvatars = ownedAvatars,
                teamSize = savedTeamSize,
                referrerUid = savedReferrer
            )
            miningService?.updateState(_state.value)
            
            if (sessionRemaining > 0) {
                startMining(isAutoResume = true)
            }
        } else {
            val initialTimestamp = user.metadata?.creationTimestamp ?: System.currentTimeMillis()
            _state.value = _state.value.copy(joinedTimestamp = initialTimestamp)
            syncAllDataToFirebase()
        }
        startSyncLoop(user.uid)
        return cloudData
    }

    private fun startSyncLoop(userId: String) {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                syncAllDataToFirebase()
            }
        }
    }

    private fun checkForUnclaimedReferralRewards(userId: String) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()
            try {
                val unclaimedDividends = db.collection("referral_dividends")
                    .whereEqualTo("referrerUid", userId)
                    .whereEqualTo("claimedByReferrer", false)
                    .get().await()

                if (!unclaimedDividends.isEmpty) {
                    val batch = db.batch()
                    var totalBonus = 0.0

                    for (doc in unclaimedDividends.documents) {
                        val bonus = doc.getDouble("bonusAmount") ?: 0.0
                        totalBonus += bonus
                        batch.update(doc.reference, "claimedByReferrer", true)
                    }

                    val userRef = db.collection("users").document(userId)
                    batch.update(userRef, "balance", FieldValue.increment(totalBonus))
                    
                    batch.commit().await()
                    
                    val newTotal = _state.value.totalMined + totalBonus
                    _state.value = _state.value.copy(totalMined = newTotal)
                    persistence.saveBalance(newTotal)
                    
                    Log.d("MiningVM", "Claimed $totalBonus ETR kickbacks from team mining!")
                }
            } catch (e: Exception) {
                Log.e("MiningVM", "Failed to check kickbacks", e)
            }
        }
    }

    fun syncAllDataToFirebase() {
        val user = auth.currentUser ?: return
        val currentState = _state.value
        val sessionEndTime = System.currentTimeMillis() + currentState.sessionTimeRemaining
        
        val data: Map<String, Any> = mapOf(
            "balance" to currentState.totalMined,
            "username" to currentState.username,
            "hashrate" to currentState.hashrate,
            "equipmentTier" to currentState.equipment.tier.name,
            "streak" to currentState.streak,
            "teamSize" to currentState.teamSize,
            "referrerUid" to (currentState.referrerUid ?: ""),
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
            "dataSdkOptIn" to currentState.isDataSdkOptedIn,
            "joinedTimestamp" to currentState.joinedTimestamp,
            "lifetimeMined" to currentState.lifetimeMined,
            "maxHashrate" to currentState.maxHashrate,
            "profilePictureUrl" to (currentState.profilePictureUrl ?: ""),
            "ownedAvatars" to currentState.ownedAvatars
        )

        viewModelScope.launch {
            sync.uploadUserData(user.uid, data)
        }
    }

    fun startMining(isAutoResume: Boolean = false) {
        if (!isAutoResume) {
            // Fresh click: Reset earnings and timer to 4 hours
            _state.value = _state.value.copy(
                sessionTimeRemaining = SESSION_DURATION,
                sessionEarnings = 0.0
            )
        } else {
            // Auto-resume: KEEP the existing sessionTimeRemaining from state
            if (_state.value.sessionTimeRemaining <= 0) return
        }

        miningService?.updateState(_state.value)
        val intent = Intent(appContext, MiningService::class.java).apply { 
            action = "START" 
            putExtra("remaining_time", _state.value.sessionTimeRemaining)
            putExtra("session_earnings", _state.value.sessionEarnings)
        }
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
        if (amount == 0.0) return
        
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

    suspend fun updateProfilePicture(uri: Uri): Boolean {
        val userId = _state.value.userId ?: return false
        val downloadUrl = storage.uploadProfilePicture(userId, uri)
        if (downloadUrl != null) {
            val success = auth.updatePhotoUri(Uri.parse(downloadUrl))
            if (success) {
                _state.value = _state.value.copy(profilePictureUrl = downloadUrl)
                syncAllDataToFirebase()
                return true
            }
        }
        return false
    }

    suspend fun updateSelectedAvatar(avatarName: String) {
        val userId = _state.value.userId ?: return
        _state.value = _state.value.copy(profilePictureUrl = avatarName)
        syncAllDataToFirebase()
    }

    suspend fun purchaseAvatar(avatarName: String, cost: Double): Boolean {
        val currentBalance = _state.value.totalMined
        if (currentBalance >= cost) {
            val newBalance = currentBalance - cost
            val newOwnedList = _state.value.ownedAvatars + avatarName
            persistence.saveBalance(newBalance)
            
            _state.value = _state.value.copy(
                totalMined = newBalance,
                ownedAvatars = newOwnedList,
                profilePictureUrl = avatarName
            )
            
            recordTransaction("AVATAR_PURCHASE", -cost)
            syncAllDataToFirebase()
            return true
        }
        return false
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
        miningService?.updateState(_state.value)
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
        notificationListener?.remove()
        super.onCleared() 
    }
}
