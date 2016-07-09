package torr.client

import be.adaxisoft.bencode.BDecoder
import nl.komponents.kovenant.task
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.net.ConnectException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by adrian on 07.07.16.
 */

/**
 * handshake: <pstrlen><pstr><reserved><info_hash><peer_id>
 */
class HandshakeMessage {
    // handshake: <pstrlen><pstr><reserved><info_hash><peer_id>

    fun decode(input: DataInputStream): HandShake {


//        var len = (-1).toByte()
//        try {
//            val len = input.readByte()
//        } catch (eof: EOFException) {
//            // No Data available
//
//        }


        var pstrlen:Byte? = null

//        try {
//            pstrlen = input.readByte()
//        } catch (eof : EOFException) {
//            // Nothing to read
//
//        }

        while(pstrlen == null) {
            try {
                pstrlen = input.readByte()
            } catch (eof : EOFException) {
                // Nothing to read
            }
        }
//        val pstrlen = input.readByte()

        val msgLen = 49+pstrlen!!
        println("Handshake msg len: $msgLen")

        if (pstrlen != 19.toByte()) {
            throw IllegalStateException("pstrlen in peer Handshake response is not 19, but $pstrlen")
        }

        val b = ArrayList<Byte>()
        b.add(pstrlen)

        // fill buffer
        while(b.size < msgLen) {
            b.add(input.readByte())
        }

        val pstr = b.slice(1..19).toByteArray()
        val reserved = b.slice(20..27).toByteArray()
        val infoHash = b.slice(28..47).toByteArray()
        val peerId = b.slice(48..67).toByteArray()

        val hs = HandShake(pstrlen.toInt(), String(pstr), reserved, infoHash, String(peerId))
        println(hs)
        return hs

    }

    fun encode(handShake: HandShake): ByteArray {

        val bytes = ByteBuffer.allocate(68)
                .put(handShake.pstrlen.toByte())
                .put(handShake.pstr.toByteArray())
                .put(handShake.reserved)
                .put(handShake.infoHash)
                .put(handShake.peerId.toByteArray())

        return bytes.array()
    }
}


/**
 * keep-alive: <len=0000>
 */
class KeepAliveMessage {

    fun decode(bytes: ByteArray) {
        if (!Arrays.equals(ByteArray(4), bytes)) throw IllegalArgumentException("Invalid Keepalive Message")
    }

    fun encode() = ByteArray(4)
}

/**
 * choke: <len=0001><id=0>
 */
class ChockedMessage {
    val MSG_ID = 0.toByte()
    fun decode(payload: ByteArray) {
        if (payload.size != 3 || payload[4] != MSG_ID) throw IllegalArgumentException("Invalid ChockeMessage")
    }

    fun encode() = byteArrayOf(0, 0, 0, 1, 0)

}

/**
 * unchoke: <len=0001><id=1>
 */
class UnChockedMessage {
    val MSG_ID = 1.toByte()

    fun decode(bytes: ByteArray) {
        if (bytes.size != 5 || bytes[4] != MSG_ID) throw IllegalArgumentException("Invalid UnChockeMessage")
    }

    fun encode() = byteArrayOf(0, 0, 0, 1, MSG_ID)
}

/**
 * interested: <len=0001><id=2>
 */
class InterestedMessage {
    val MSG_ID = 2.toByte()

    fun decode(bytes: ByteArray) {
        if (bytes.size != 5 || bytes[4] != MSG_ID) throw IllegalArgumentException("Invalid InterestedMessage")
    }

    fun encode() = byteArrayOf(0, 0, 0, 1, MSG_ID)
}

/**
 * not interested: <len=0001><id=3>
 */
class NotInterestedMessage {
    val MSG_ID = 3.toByte()

    fun decode(bytes: ByteArray) {
        if (bytes.size != 5 || bytes[4] != MSG_ID) throw IllegalArgumentException("Invalid NotInterestedMessage")
    }

    fun encode() = byteArrayOf(0, 0, 0, 1, MSG_ID)
}

/**
 * have: <len=0005><id=4><piece index>
 */
class HaveMessage {
    val MSG_ID = 4.toByte()

    fun decode(inputStream: DataInputStream): Int {
        return inputStream.readInt()
    }

    fun encode(pieceIndex: Int): ByteArray {
        val buffer = ByteBuffer.allocate(9)
        buffer.put(ByteArray(4))
        buffer.put(MSG_ID)
        buffer.putInt(pieceIndex)
        return buffer.array()
    }
}

/**
 * bitfield: <len=0001+X><id=5><bitfield>
 */
class BitFieldMessage {
    val MSG_ID = 5.toByte()

    fun decode(inputStream: DataInputStream, bitfieldLength: Int): BooleanArray {

        val buffer = ByteArray(bitfieldLength)
        inputStream.readFully(buffer)

        val res = BooleanArray(bitfieldLength)
        for (i in buffer.indices) {
            res[i] = buffer [i] == 1.toByte()
        }


        return res
    }

    fun encode(): ByteArray {
        throw IllegalArgumentException("TODO implenent BitFieldMessage")

        val buffer = ByteBuffer.allocate(9)
        return buffer.array()
    }
    // TODO
}

/**
 * request: <len=0013><id=6><index><begin><length>
 */
class RequestMessage {
    val MSG_ID = 6.toByte()

    fun decode(bytes: ByteArray) {
        if (bytes.size != 17 || bytes[4] != MSG_ID) throw IllegalArgumentException("Invalid RequestMessage")
    }

    fun encode(pieceIndex: Int, begin: Int, length: Int = 16384): ByteArray {
        val buffer = ByteBuffer.allocate(17)
        buffer.put(ByteArray(4))
        buffer.put(MSG_ID)
        buffer.putInt(pieceIndex)
        buffer.putInt(begin)
        buffer.putInt(length)
        return buffer.array()
    }
}

/**
 * piece: <len=0009+X><id=7><index><begin><block>
 */
class PieceMessage {
    val MSG_ID = 7.toByte()

    fun decode(bytes: ByteArray) {
        if (bytes.size < 13 || bytes[4] != MSG_ID) throw IllegalArgumentException("Invalid PieceMessage")
    }

    fun encode(pieceIndex: Int, begin: Int, block: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(17) //TODO
        buffer.put(ByteArray(4))
        buffer.put(MSG_ID)
        buffer.putInt(pieceIndex)
        buffer.putInt(begin)
        buffer.put(block)
        return buffer.array()
    }
}

/**
 * cancel: <len=0013><id=8><index><begin><length>
 */
class CancelMessage {
    val MSG_ID = 8.toByte()

    fun decode(bytes: ByteArray) {
        if (bytes.size != 17 || bytes[4] != MSG_ID) throw IllegalArgumentException("Invalid RequestMessage")
    }

    fun encode(pieceIndex: Int, begin: Int, length: Int): ByteArray {
        val buffer = ByteBuffer.allocate(9)
        buffer.put(ByteArray(4))
        buffer.put(MSG_ID)
        buffer.putInt(pieceIndex)
        buffer.putInt(begin)
        buffer.putInt(length)
        return buffer.array()
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

    if (response.failure_reason != null) {
        println("FAILED: ${response.failure_reason}")
        return
    }

    if (response.warning_message != null) {
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

        if (ipAddr == "178.195.191.249") continue // It's me

        val port1 = (peers[i + 4].toInt() and 0xff)
        val port2 = (peers[i + 5].toInt() and 0xff)

        // http://stackoverflow.com/questions/1026761/how-to-convert-a-byte-array-to-its-numeric-value-java
//        val port = (peers[i+4].toInt() and 0xFF shl 8) or (peers[i+5].toInt() and 0xFF)
        val port = (port1 * 256) + port2

        println("Peer Info from Tracker[${i / 6 + 1} / ${peers.size / 6}]: $ipAddr : $port")


        // TODO: send / receive only after successfull connect

        val init = task {
            println("TASK Setup start")
            val peer = Peer(ipAddr)
            try {
                peer.setUpConnection(ipAddr, port, infoHash)
//                peer.handshake(infoHash)
            } catch (c : ConnectException) {
                c.printStackTrace()
            }

            println("TASK Setup end")
            peer
        } fail {
            print("FAIL")
        }

        val peer = init.get()


        println("success ${peer.ipAddr}")

        task {
            while (true) {
//                println("REC")
                TimeUnit.SECONDS.sleep(1)
                MessageReceiver(peer).decodeIncomingMessage()


            }
        }

        task {
            while (true) {
//                println("SEND")
                TimeUnit.SECONDS.sleep(1)
                MessageSender(peer).sendMessage()



            }
        }

    }


}
