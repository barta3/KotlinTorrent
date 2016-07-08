package torr.client

import be.adaxisoft.bencode.BDecoder
import java.io.DataInputStream
import java.io.InputStream
import java.net.ConnectException
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Created by adrian on 07.07.16.
 */

class HandshakeMessage {
    // handshake: <pstrlen><pstr><reserved><info_hash><peer_id>

    fun decode(input: DataInputStream): HandShake {

        val pstrlen = input.readByte()

        if(pstrlen != 19.toByte()) {
            throw IllegalStateException("pstrlen in peer Handshake response is not 19")
        }

        val pstr = ByteArray(19)
        input.readFully(pstr)

        val reserved = ByteArray(8)
        input.readFully(reserved)

        val infoHash = ByteArray(20)
        input.readFully(infoHash)

        val peerId = ByteArray(20)
        input.readFully(peerId)

        return  HandShake(pstrlen.toInt(), String(pstr, Charset.forName("UTF-8")), reserved, infoHash, String(peerId, Charset.forName("UTF-8")))

    }

    fun encode(handShake: HandShake) : ByteArray {

        val bytes = ByteBuffer.allocate(68)
        .put(handShake.pstrlen.toByte())
        .put(handShake.pstr.toByteArray())
        .put(handShake.reserved)
        .put(handShake.infoHash)
        .put(handShake.peerId.toByteArray())

        return bytes.array()
    }
}

fun parseTrackerResponse(ins: InputStream, infoHash: ByteArray) {
    val map = BDecoder(ins).decodeMap().map
    println(map)
    ins.close()

    val response = TrackerResponse(
            failure_reason = map["failure reason"]?.string,
            warning_message = map["warning message"]?.string,
            interval = map["interval"]?.int,
            min_interval = map["min interval"]?.int,
            tracker_id = map["tracker id"]?.string,
            complete = map["complete"]?.int,
            incomplete = map["incomplete"]?.int,
            peers = map["peers"]?.bytes
    )

    println("Tracker response: " + response)

    if(response.failure_reason != null) {
        println("FAILED: ${response.failure_reason}")
        return
    }

    if(response.warning_message != null) {
        println("WARNING: ${response.warning_message}")
    }

    val peers = response.peers!!

    var value: Long = 0
    for (i in 0..peers.size - 1) {
        value = (value shl 8) + (peers[i].toInt() and 0xff)
    }

    for (i in peers.indices) {
        if (i % 6 != 0) continue

        val ipAddr = "${(peers[i].toInt() and 0xff)}.${(peers[i + 1].toInt() and 0xff)}.${(peers[i + 2].toInt() and 0xff)}.${(peers[i + 3].toInt() and 0xff)}"


        val port1 = (peers[i + 4].toInt() and 0xff)
        val port2 = (peers[i + 5].toInt() and 0xff)

        // http://stackoverflow.com/questions/1026761/how-to-convert-a-byte-array-to-its-numeric-value-java
//        val port = (peers[i+4].toInt() and 0xFF shl 8) or (peers[i+5].toInt() and 0xFF)
        val port = (port1 * 256) + port2

        println("Peer Info from Tracker: $ipAddr : $port")

        try {
            Thread(PeerHandler(ipAddr, port, infoHash)).start()
        } catch (e: ConnectException) {
            println("ERROR for $ipAddr : $port")
            e.printStackTrace()
        }


    }
}