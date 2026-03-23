package com.zapret.vpn.model

/**
 * Runtime profile that can be swapped without leaking engine implementation details to the UI.
 */
data class TunnelProfile(
    val sessionName: String = "zapret-next",
    val mtu: Int = 1500,
    val vpnIpv4: String = "10.60.0.2",
    val vpnPrefixLength: Int = 24,
    val dnsServers: List<String> = listOf("1.1.1.1", "8.8.8.8"),
    val bypassPackages: Set<String> = emptySet(),
    val enableIpv6: Boolean = false,
    val routeAllTraffic: Boolean = true,
)
