package com.zapret.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zapret.vpn.engine.TrafficEngine
import com.zapret.vpn.engine.UserspaceTrafficEngine
import com.zapret.vpn.model.TunnelProfile
import com.zapret.vpn.policy.VpnBatteryPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ForegroundZapretVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var batteryPolicy: VpnBatteryPolicy
    private var trafficEngine: TrafficEngine? = null

    override fun onCreate() {
        super.onCreate()
        batteryPolicy = VpnBatteryPolicy(this)
        batteryPolicy.ensureNotificationChannel(VpnBatteryPolicy.CHANNEL_ID, "Zapret VPN")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_START,
            ACTION_RESTART,
            -> startTunnel()

            ACTION_STOP -> stopTunnel(stopService = true)
        }
        return START_STICKY
    }

    override fun onRevoke() {
        stopTunnel(stopService = true)
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnel(stopService = false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    private fun startTunnel() {
        if (trafficEngine != null) return
        startForegroundCompat()

        val profile = TunnelProfile()
        val builder = Builder()
            .setSession(profile.sessionName)
            .setMtu(profile.mtu)
            .addAddress(profile.vpnIpv4, profile.vpnPrefixLength)

        profile.dnsServers.forEach { dns -> builder.addDnsServer(dns) }

        if (profile.routeAllTraffic) {
            builder.addRoute("0.0.0.0", 0)
        }

        profile.bypassPackages.forEach { pkg ->
            runCatching { builder.addDisallowedApplication(pkg) }
        }

        val tun = builder.establish() ?: return
        val engine = UserspaceTrafficEngine(tun)
        trafficEngine = engine

        serviceScope.launch {
            engine.start(profile)
        }
    }

    private fun stopTunnel(stopService: Boolean) {
        val engine = trafficEngine ?: return
        serviceScope.launch {
            runCatching { engine.stop() }
            trafficEngine = null
            if (stopService) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                batteryPolicy.scheduleRestart()
            }
        }
    }

    private fun startForegroundCompat() {
        val notification = buildStickyNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                VpnBatteryPolicy.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(VpnBatteryPolicy.NOTIFICATION_ID, notification)
        }
    }

    private fun buildStickyNotification(): Notification {
        val stopIntent = Intent(this, ForegroundZapretVpnService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, VpnBatteryPolicy.CHANNEL_ID)
            .setContentTitle("Zapret VPN is active")
            .setContentText("Traffic tunnel is running in foreground mode")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.zapret.vpn.action.START"
        const val ACTION_STOP = "com.zapret.vpn.action.STOP"
        const val ACTION_RESTART = "com.zapret.vpn.action.RESTART"
    }
}
