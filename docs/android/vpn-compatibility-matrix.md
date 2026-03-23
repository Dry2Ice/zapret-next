# Android VPN Compatibility Matrix

| Android version | API | Support status | Notes | Test status |
|---|---:|---|---|---|
| Android 8.0/8.1 (Oreo) | 26-27 | ✅ Supported | Baseline for foreground service + sticky notification. | ⚠️ Manual validation required |
| Android 9 (Pie) | 28 | ✅ Supported | Background execution limits require START_STICKY strategy. | ⚠️ Manual validation required |
| Android 10 (Q) | 29 | ✅ Supported | `foregroundServiceType` used for VPN lifecycle transparency. | ⚠️ Manual validation required |
| Android 11 (R) | 30 | ✅ Supported | Scoped storage has no direct impact to VPN path. | ⚠️ Manual validation required |
| Android 12 (S) | 31 | ✅ Primary target | PendingIntent mutability set explicitly. | ✅ Unit-test profile/runtime checks |
| Android 13 (Tiramisu) | 33 | ✅ Primary target | Notification runtime consent can impact first launch UX. | ✅ Unit-test profile/runtime checks |
| Android 14+ (Upside Down Cake+) | 34+ | ✅ Primary target | Verify foreground service policy and restart alarm behavior. | ✅ Unit-test profile/runtime checks |

## Mandatory regression suite

1. Tunnel bring-up and teardown (`ACTION_START` / `ACTION_STOP`).
2. TUN packet loop sanity for IPv4 TCP/UDP frames.
3. Sticky foreground notification visibility during active tunnel.
4. Restart strategy after process death (`ACTION_RESTART`, alarm + boot receiver).
5. `TrafficEngine` API contract: `start`, `stop`, `applyProfile`, `getStats`.
