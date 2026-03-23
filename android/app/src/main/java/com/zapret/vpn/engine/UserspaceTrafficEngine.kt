package com.zapret.vpn.engine

import android.os.ParcelFileDescriptor
import com.zapret.vpn.model.TrafficStats
import com.zapret.vpn.model.TunnelProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Kotlin userspace pipeline for TUN packet read/write and basic flow accounting.
 * Can be swapped later with a JNI-backed implementation without touching UI/service code.
 */
class UserspaceTrafficEngine(
    private val tunFd: ParcelFileDescriptor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TrafficEngine {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val packetsOut = AtomicLong(0)
    private val packetsIn = AtomicLong(0)
    private val bytesOut = AtomicLong(0)
    private val bytesIn = AtomicLong(0)
    private val activeFlows = ConcurrentHashMap.newKeySet<FlowKey>()
    private val startedAt = AtomicLong(0)

    @Volatile
    private var currentProfile: TunnelProfile? = null
    private var workerJob: Job? = null

    override suspend fun start(profile: TunnelProfile) {
        if (workerJob?.isActive == true) return
        currentProfile = profile
        startedAt.set(System.currentTimeMillis())
        workerJob = scope.launch { loopTunPackets() }
    }

    override suspend fun stop() {
        workerJob?.cancelAndJoin()
        workerJob = null
        activeFlows.clear()
        runCatching { tunFd.close() }
    }

    override suspend fun applyProfile(profile: TunnelProfile) {
        currentProfile = profile
    }

    override fun getStats(): TrafficStats = TrafficStats(
        bytesIn = bytesIn.get(),
        bytesOut = bytesOut.get(),
        packetsIn = packetsIn.get(),
        packetsOut = packetsOut.get(),
        activeFlows = activeFlows.size,
        startedAtMs = startedAt.get(),
    )

    private suspend fun loopTunPackets() = withContext(dispatcher) {
        FileInputStream(tunFd.fileDescriptor).use { input ->
            FileOutputStream(tunFd.fileDescriptor).use { output ->
                val packetBuffer = ByteArray(MAX_PACKET_SIZE)
                while (isActive) {
                    ensureActive()
                    val read = input.read(packetBuffer)
                    if (read <= 0) continue

                    bytesOut.addAndGet(read.toLong())
                    packetsOut.incrementAndGet()

                    val packet = PacketParser.parse(ByteBuffer.wrap(packetBuffer, 0, read)) ?: continue
                    activeFlows.add(packet.flowKey)

                    // Baseline userspace routing path:
                    //  - TCP/UDP packets go through the same pipeline stage for now.
                    //  - Future JNI integration can replace this with native forwarding.
                    when (packet.protocol) {
                        IpProtocol.TCP,
                        IpProtocol.UDP,
                        -> {
                            output.write(packet.raw)
                            bytesIn.addAndGet(packet.raw.size.toLong())
                            packetsIn.incrementAndGet()
                        }

                        else -> {
                            // Ignore non TCP/UDP by design in the baseline version.
                        }
                    }
                }
            }
        }
    }

    private companion object {
        private const val MAX_PACKET_SIZE = 32 * 1024
    }
}
