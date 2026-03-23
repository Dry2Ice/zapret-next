package com.zapret.vpn.policy

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zapret.vpn.ForegroundZapretVpnService

class VpnBatteryPolicy(private val context: Context) {

    fun ensureNotificationChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Foreground VPN tunnel state"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun scheduleRestart(delayMs: Long = DEFAULT_RESTART_DELAY_MS) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val restartIntent = Intent(context, ForegroundZapretVpnService::class.java).apply {
            action = ForegroundZapretVpnService.ACTION_RESTART
        }
        val pendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE_RESTART,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delayMs,
            pendingIntent,
        )
    }

    companion object {
        const val CHANNEL_ID = "zapret_vpn_foreground"
        const val NOTIFICATION_ID = 14001
        private const val REQUEST_CODE_RESTART = 14002
        const val DEFAULT_RESTART_DELAY_MS = 2_500L
    }
}
