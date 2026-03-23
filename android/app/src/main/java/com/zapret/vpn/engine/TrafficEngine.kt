package com.zapret.vpn.engine

import com.zapret.vpn.model.TrafficStats
import com.zapret.vpn.model.TunnelProfile

/**
 * Stable boundary between UI/service and the packet processing implementation.
 */
interface TrafficEngine {
    suspend fun start(profile: TunnelProfile)
    suspend fun stop()
    suspend fun applyProfile(profile: TunnelProfile)
    fun getStats(): TrafficStats
}
