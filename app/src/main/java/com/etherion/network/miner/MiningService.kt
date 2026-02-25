package com.etherion.network.miner

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.etherion.network.auth.AuthManager
import com.etherion.network.domain.MiningEconomyManager
import com.example.etherion3.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class MiningService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var miningJob: Job? = null
    
    private val persistence by lazy { MiningPersistence(this) }
    private val economy = MiningEconomyManager()
    private val auth = AuthManager()

    private val _serviceState = MutableStateFlow(MiningViewModel.MiningState())
    val serviceState: StateFlow<MiningViewModel.MiningState> = _serviceState

    inner class LocalBinder : Binder() {
        fun getService(): MiningService = this@MiningService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }
        val balance = persistence.loadBalance()
        _serviceState.value = _serviceState.value.copy(totalMined = balance)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startMining()
            "STOP" -> stopMining()
        }
        return START_STICKY
    }

    private fun startMining() {
        if (miningJob != null) return
        
        val sessionDuration = 4 * 60 * 60 * 1000L
        
        if (_serviceState.value.sessionTimeRemaining <= 0) {
            _serviceState.value = _serviceState.value.copy(
                sessionTimeRemaining = sessionDuration,
                sessionEarnings = 0.0
            )
        }
        
        // Start foreground with a static, silent notification
        val notification = createNotification("Mining Active", "Etherion Node is accumulating ETR in background.")
        startForeground(1, notification)

        miningJob = serviceScope.launch {
            _serviceState.value = _serviceState.value.copy(isMining = true)

            while (isActive && _serviceState.value.sessionTimeRemaining > 0) {
                val state = _serviceState.value
                val sessionCap = economy.getSessionScarcityCap(state.equipment.tier)
                val boostMultiplier = if (state.adBoostActive) 1.5 else 1.0
                
                val hashrate = economy.calculateHashrate(
                    equipment = state.equipment, 
                    streakDays = state.streak,
                    integrity = state.equipment.integrity,
                    adBoostMultiplier = boostMultiplier,
                    regionalMultiplier = state.regionalMultiplier,
                    dataSdkBoost = state.isDataSdkOptedIn
                )
                
                val tickEarnings = if (state.sessionEarnings < sessionCap) {
                    economy.calculateEarnings(hashrate) / 2.0
                } else {
                    0.0
                }
                
                val newSessionEarnings = state.sessionEarnings + tickEarnings
                val newIntegrity = (state.equipment.integrity - 0.00001).coerceAtLeast(0.0)
                val updatedEquipment = state.equipment.copy(integrity = newIntegrity)
                
                val newSessionTime = maxOf(0L, state.sessionTimeRemaining - 1000)
                val newBoostTime = if (state.adBoostActive) maxOf(0L, state.adBoostTimeRemaining - 1000) else 0L
                val isBoostStillActive = newBoostTime > 0

                _serviceState.value = state.copy(
                    isMining = true,
                    sessionEarnings = newSessionEarnings,
                    hashrate = hashrate,
                    equipment = updatedEquipment,
                    sessionTimeRemaining = newSessionTime,
                    adBoostActive = isBoostStillActive,
                    adBoostTimeRemaining = newBoostTime,
                    status = if (newSessionEarnings >= sessionCap) "Cap Reached" else "Mining Active"
                )

                // REMOVED: updateNotification() call from every second to avoid constant alerts

                if (newSessionTime <= 0) {
                    finalizeSession()
                    showSessionEndedNotification()
                    stopMining()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun showSessionEndedNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("auto_start_miner", true)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // This uses the 'mining_alerts' channel which is HIGH importance to wake the user up
        val builder = NotificationCompat.Builder(this, "mining_alerts")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Mining Session Ended")
            .setContentText("Your node is now offline. Tap to restart and continue earning ETR.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .addAction(android.R.drawable.ic_media_play, "RENEW SESSION", pendingIntent)

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2, builder.build())
    }

    private fun stopMining() {
        serviceScope.launch {
            finalizeSession()
            miningJob?.cancel()
            miningJob = null
            _serviceState.value = _serviceState.value.copy(
                isMining = false, 
                hashrate = 0.0, 
                status = "Ready",
                sessionTimeRemaining = 0L
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun finalizeSession() {
        val state = _serviceState.value
        val earnings = state.sessionEarnings
        if (earnings <= 0) return

        val currentBalance = persistence.loadBalance()
        val newBalance = currentBalance + earnings
        persistence.saveBalance(newBalance)

        val userId = auth.getUserId()
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val tx = mapOf(
                "type" to "MINING_SESSION",
                "amount" to earnings,
                "timestamp" to System.currentTimeMillis(),
                "duration" to "4h"
            )
            try {
                db.collection("users").document(userId).collection("rewards").add(tx).await()
                db.collection("users").document(userId).update("balance", newBalance).await()
            } catch (e: Exception) {
                Log.e("MiningService", "Failed to sync payout", e)
            }
        }

        _serviceState.value = _serviceState.value.copy(
            totalMined = newBalance,
            sessionEarnings = 0.0
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Channel 1: Static background status (MIN importance = No sound, no icon in status bar overlay)
        val miningChannel = NotificationChannel(
            "mining_status_quiet_v3",
            "Background Mining Status",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Silent status for active mining (Static)"
            setShowBadge(false)
        }
        manager.createNotificationChannel(miningChannel)

        // Channel 2: High importance alerts for session end
        val alertChannel = NotificationChannel(
            "mining_alerts",
            "Mining Session Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for when mining sessions end."
            enableVibration(true)
        }
        manager.createNotificationChannel(alertChannel)
    }

    private fun createNotification(title: String, content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "mining_status_quiet_v3")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        // Now only used for rare state changes, not every second
        val notification = createNotification(title, content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    private fun formatTime(ms: Long): String {
        val hours = ms / (1000 * 60 * 60)
        val minutes = (ms / (1000 * 60)) % 60
        val seconds = (ms / 1000) % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
    
    fun updateState(newState: MiningViewModel.MiningState) {
        _serviceState.value = newState
    }
}
