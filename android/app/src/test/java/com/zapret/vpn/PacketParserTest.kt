package com.zapret.vpn

import com.zapret.vpn.engine.IpProtocol
import com.zapret.vpn.engine.PacketParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer

class PacketParserTest {

    @Test
    fun `parse identifies tcp flow`() {
        val packet = buildIpv4Packet(protocol = 6, srcPort = 443, dstPort = 52000)
        val parsed = PacketParser.parse(ByteBuffer.wrap(packet))

        assertNotNull(parsed)
        assertEquals(IpProtocol.TCP, parsed?.protocol)
        assertEquals(443, parsed?.flowKey?.srcPort)
        assertEquals(52000, parsed?.flowKey?.dstPort)
    }

    @Test
    fun `parse identifies udp flow`() {
        val packet = buildIpv4Packet(protocol = 17, srcPort = 53, dstPort = 53000)
        val parsed = PacketParser.parse(ByteBuffer.wrap(packet))

        assertNotNull(parsed)
        assertEquals(IpProtocol.UDP, parsed?.protocol)
        assertEquals(53, parsed?.flowKey?.srcPort)
        assertEquals(53000, parsed?.flowKey?.dstPort)
    }

    private fun buildIpv4Packet(protocol: Int, srcPort: Int, dstPort: Int): ByteArray {
        val raw = ByteArray(40)
        raw[0] = 0x45
        raw[9] = protocol.toByte()
        raw[12] = 10
        raw[13] = 60
        raw[14] = 0
        raw[15] = 2
        raw[16] = 1
        raw[17] = 1
        raw[18] = 1
        raw[19] = 1
        raw[20] = ((srcPort shr 8) and 0xFF).toByte()
        raw[21] = (srcPort and 0xFF).toByte()
        raw[22] = ((dstPort shr 8) and 0xFF).toByte()
        raw[23] = (dstPort and 0xFF).toByte()
        return raw
    }
}
