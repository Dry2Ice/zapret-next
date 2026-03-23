package com.zapret.vpn.engine

import java.net.InetAddress
import java.nio.ByteBuffer

enum class IpProtocol(val number: Int) {
    TCP(6),
    UDP(17),
    OTHER(-1),
}

data class FlowKey(
    val srcIp: String,
    val srcPort: Int,
    val dstIp: String,
    val dstPort: Int,
    val protocol: IpProtocol,
)

data class ParsedPacket(
    val protocol: IpProtocol,
    val flowKey: FlowKey,
    val raw: ByteArray,
)

object PacketParser {
    fun parse(buffer: ByteBuffer): ParsedPacket? {
        if (buffer.remaining() < 20) return null
        val raw = ByteArray(buffer.remaining())
        buffer.get(raw)
        val packet = ByteBuffer.wrap(raw)

        val version = (packet.get(0).toInt() ushr 4) and 0x0F
        if (version != 4) return null
        val ihl = (packet.get(0).toInt() and 0x0F) * 4
        if (raw.size < ihl + 4) return null

        val protocol = when (packet.get(9).toInt() and 0xFF) {
            IpProtocol.TCP.number -> IpProtocol.TCP
            IpProtocol.UDP.number -> IpProtocol.UDP
            else -> IpProtocol.OTHER
        }

        val srcIp = InetAddress.getByAddress(raw.copyOfRange(12, 16)).hostAddress ?: return null
        val dstIp = InetAddress.getByAddress(raw.copyOfRange(16, 20)).hostAddress ?: return null

        val srcPort = readPort(raw, ihl)
        val dstPort = readPort(raw, ihl + 2)

        return ParsedPacket(
            protocol = protocol,
            flowKey = FlowKey(srcIp, srcPort, dstIp, dstPort, protocol),
            raw = raw,
        )
    }

    private fun readPort(raw: ByteArray, offset: Int): Int {
        if (offset + 1 >= raw.size) return -1
        return ((raw[offset].toInt() and 0xFF) shl 8) or (raw[offset + 1].toInt() and 0xFF)
    }
}
