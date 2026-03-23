# VPN module overview

## Components

- `ForegroundZapretVpnService` — foreground VPN service lifecycle (`ACTION_START`, `ACTION_STOP`, `ACTION_RESTART`).
- `TrafficEngine` — stable API boundary between UI/service and traffic processing implementation.
- `UserspaceTrafficEngine` — Kotlin userspace TUN read/write pipeline with baseline TCP/UDP routing.
- `VpnBatteryPolicy` — sticky notification channel and restart scheduling to survive background/battery limits.
- `VpnRestartReceiver` — boot/package replaced trigger for tunnel restart strategy.

## API boundary

```kotlin
interface TrafficEngine {
    suspend fun start(profile: TunnelProfile)
    suspend fun stop()
    suspend fun applyProfile(profile: TunnelProfile)
    fun getStats(): TrafficStats
}
```

UI and ViewModel layers should talk only to `TrafficEngine` abstractions.
