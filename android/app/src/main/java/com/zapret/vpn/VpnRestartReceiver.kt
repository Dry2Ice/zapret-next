package com.zapret.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Entry-point for BOOT_COMPLETED / package-replaced restart strategy.
 */
class VpnRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val restartIntent = Intent(context, ForegroundZapretVpnService::class.java).apply {
                this.action = ForegroundZapretVpnService.ACTION_RESTART
            }
            context.startForegroundService(restartIntent)
        }
    }
}
