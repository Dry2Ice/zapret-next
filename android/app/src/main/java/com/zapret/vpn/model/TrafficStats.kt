package com.zapret.vpn.model

data class TrafficStats(
    val bytesIn: Long,
    val bytesOut: Long,
    val packetsIn: Long,
    val packetsOut: Long,
    val activeFlows: Int,
    val startedAtMs: Long,
)
