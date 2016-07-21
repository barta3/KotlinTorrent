package torr.client

import java.io.DataInputStream
import java.io.EOFException
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
    val LOG by lazyLogger()

    fun decode(input: DataInputStream): HandShake {


        var pstrlen:Byte? = null

        while(pstrlen == null) {
            TimeUnit.MILLISECONDS.sleep(500)
            try {
                pstrlen = input.readByte()
            } catch (eof : EOFException) {
                // Nothing to read
            }
        }

        val msgLen = 49+pstrlen!!
        LOG.info("Handshake msg len: $msgLen")

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
        LOG.info(hs.toString())
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
        buffer.putInt(5)
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
        buffer.putInt(13)
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

    fun decode(inputStream: DataInputStream, blockLen: Int): Piece {

        val pieceIndex = inputStream.readInt()
        val begin = inputStream.readInt()
        val block = ByteArray(blockLen)
        inputStream.readFully(block)
        return Piece(pieceIndex, begin, block)
    }

    fun encode(pieceIndex: Int, begin: Int, block: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(17) //TODO
        buffer.putInt(9+block.size)
        buffer.put(MSG_ID)
        buffer.putInt(pieceIndex)
        buffer.putInt(begin)
        buffer.put(block)
        return buffer.array()
    }
}

data class Piece(val index: Int, val begin: Int, val block: ByteArray)

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
        buffer.putInt(13)
        buffer.put(MSG_ID)
        buffer.putInt(pieceIndex)
        buffer.putInt(begin)
        buffer.putInt(length)
        return buffer.array()
    }
}

data class PeerResult(val ip: String, val port:Int, val infoHash: ByteArray)


