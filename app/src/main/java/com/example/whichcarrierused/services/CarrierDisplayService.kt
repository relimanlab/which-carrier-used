package com.example.whichcarrierused.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.whichcarrierused.R

class CarrierDisplayService : Service() {
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var handler: Handler
    private lateinit var updateRunnable: Runnable
    private var lastCarrierName: String? = null
    private var lastNotificationTime: Long = 0
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "carrier_info"
        private const val UPDATE_INTERVAL = 5000L // 5秒ごとに更新
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        handler = Handler(Looper.getMainLooper())
        updateRunnable = Runnable {
            displayDefaultDataCarrier()
            handler.postDelayed(updateRunnable, UPDATE_INTERVAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(updateRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    private fun displayDefaultDataCarrier() {
        try {
            if (ActivityCompat.checkSelfPermission(this, 
                android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                
                val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
                val activeSubscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(defaultDataSubId)
                
                val carrierName = activeSubscriptionInfo?.carrierName ?: 
                                 telephonyManager.simOperatorName ?: 
                                 getString(R.string.unknown_carrier)
                
                showCarrierNotification(carrierName.toString())
            }
        } catch (e: Exception) {
            Log.e("CarrierDisplayService", "Error displaying carrier info", e)
            showCarrierNotification(getString(R.string.error_getting_carrier))
        }
    }

    private fun getCarrierIcon(carrierName: String): Int {
        val firstChar = carrierName.firstOrNull()?.uppercaseChar() ?: return R.drawable.ic_network
        
        return when (firstChar) {
            'A' -> R.drawable.ic_carrier_a
            'D' -> R.drawable.ic_carrier_d
            'I' -> R.drawable.ic_carrier_i
            'K' -> R.drawable.ic_carrier_k
            'L' -> R.drawable.ic_carrier_l
            'P' -> R.drawable.ic_carrier_p
            'R' -> R.drawable.ic_carrier_r
            'S' -> R.drawable.ic_carrier_s
            'T' -> R.drawable.ic_carrier_t
            'U' -> R.drawable.ic_carrier_u
            'V' -> R.drawable.ic_carrier_v
            'Y' -> R.drawable.ic_carrier_y
            else -> R.drawable.ic_network
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH  // 重要度を高に設定
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun showCarrierNotification(carrierName: String) {
        try {
            val iconResId = getCarrierIcon(carrierName)
            val currentTime = System.currentTimeMillis()
            
            // キャリア名が変更された場合のみ時間を更新
            if (carrierName != lastCarrierName) {
                lastCarrierName = carrierName
                lastNotificationTime = currentTime
            }

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(carrierName)
                .setPriority(NotificationCompat.PRIORITY_MAX)  // 優先度を最大に設定
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setVibrate(null)
                .setWhen(lastNotificationTime)
                .setShowWhen(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("CarrierDisplayService", "Error showing notification", e)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_network)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(carrierName)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setVibrate(null)
                .setWhen(lastNotificationTime)
                .setShowWhen(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}