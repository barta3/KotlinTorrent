package torr.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.*

/**
 * Created by adrian on 07.07.16.
 */
class HandshakeMessageTest {
    @Test
    fun decode() {

        val bytes = byteArrayOf(19.toByte(), // pstrlen
                66, 105, 116, 84, 111, 114, 114, 101, 110, 116, 32, 112, 114, 111, 116, 111, 99, 111, 108, // "BitTorrent protocol".
                0, 0, 0, 0, 0, 16, 0, 5, // reserved
                110, -56, 94, 113, 112, 103, -21, -31, -68, 95, -49, 44, 1, 17, -46, -128, 13, -123, -88, 13, // infohash
                45, 68, 69, 49, 51, 67, 48, 45, 68, 86, 53, 102, 104, 114, 114, 41, 101, 111, 49, 33) // "-DE13C0-DV5fhrr)eo1!"

        assertEquals(68, bytes.size)
        val dataInputStream = DataInputStream(ByteArrayInputStream(bytes))
        val handShake = HandshakeMessage().decode(dataInputStream)



        println(handShake)

        // handshake: <pstrlen><pstr><reserved><info_hash><peer_id>
        assertEquals(19, handShake.pstrlen)
        assertEquals("BitTorrent protocol", handShake.pstr)
        assertTrue(Arrays.equals(byteArrayOf(0, 0, 0, 0, 0, 16, 0, 5), handShake.reserved))
        assertTrue(Arrays.equals(byteArrayOf(110, -56, 94, 113, 112, 103, -21, -31, -68, 95, -49, 44, 1, 17, -46, -128, 13, -123, -88, 13), handShake.infoHash))
        assertEquals("-DE13C0-DV5fhrr)eo1!", handShake.peerId)

    }

    @Test
    fun encode() {

        val handShake = HandShake(
                pstrlen = 19,
                pstr = "BitTorrent protocol",
                reserved = ByteArray(8),
                infoHash = byteArrayOf(110, -56, 94, 113, 112, 103, -21, -31, -68, 95, -49, 44, 1, 17, -46, -128, 13, -123, -88, 13),
                peerId = "-DE13C0-DV5fhrr29eo1"
        )

        val encoded = HandshakeMessage().encode(handShake)

        assertEquals(68, encoded.size)
    }

}