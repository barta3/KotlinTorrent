package torr.client

import main.kotlin.torr.client.TorrentDecoder
import org.apache.http.client.utils.URIBuilder
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.util.*

/**
 * Created by adrian on 05.07.16.
 */


const val PEER_ID = "-KB1000-ce43pvhlofit"


fun connectToTracker(torrentFile: TorrentDecoder.TorrentFile, info: ByteArray) {

    val infoHash = hashInfoValue(info)


    val url = URL(buildTrackerUrl(torrentFile.announce, torrentFile.calculateTotalSize(), infoHash))

    val con = url.openConnection() as HttpURLConnection

    // optional default is GET
    con.requestMethod = "GET"


    println("\nSending 'GET' request to URL : " + url.toURI())
    println("Response Code : " + con.responseCode)

    parseTrackerResponse(ins = con.inputStream, infoHash = infoHash)

}

class Peer(val ipAddr: String) { //TODO: change to id

    var am_choking = true
    var am_interested = false
    var peer_choking = true
    var peer_interested = false

    // TODO: better initialisation / null handling / lazy?
    var dataInputStream: DataInputStream? = null
    var dataOutputStream: DataOutputStream? = null

    fun setUpConnection(peerIp: String, peerPort: Int, infoHash: ByteArray) {

        log("Connecting...")
        val socket = Socket(peerIp, peerPort)

        log("Connected")

        dataInputStream = DataInputStream(socket.inputStream)
        dataOutputStream = DataOutputStream(socket.outputStream)

        handshake(infoHash)
    }

    private fun handshake(infoHash: ByteArray) : Boolean {

        dataOutputStream!!.write(HandshakeMessage().encode(HandShake(infoHash = infoHash)))
        val handShakeResult = HandshakeMessage().decode(dataInputStream!!)

        log("Infohash Check: : ${Arrays.equals(infoHash, handShakeResult.infoHash)}")

        return Arrays.equals(infoHash, handShakeResult.infoHash)
    }

    private fun log(msg: String) = println("Peer ${ipAddr}: $msg")

}


class MessageReceiver(val peer: Peer) {

    fun decodeIncomingMessage() {

        var len = (-1).toByte()


        while(len < 0) {
            try {
                len = peer.dataInputStream!!.readByte()
            } catch (eof : EOFException) {
                // No Data available
            }
        }

        var msgType: Byte = (-1).toByte()

        if (len != 0.toByte()) {
            msgType = peer.dataInputStream!!.readByte()
        }


        log("decodeIncomingMessage: len: $len Type: $msgType")

        when (msgType) {
            ((-1).toByte()) -> log("Keepalive")

            (ChockedMessage().MSG_ID) -> {
                log("ChockedMessage")
                peer.peer_choking = true
            }
            (UnChockedMessage().MSG_ID) -> {
                log("UnChockedMessage")
                peer.peer_choking = false
            }
            (InterestedMessage().MSG_ID) -> {
                log("InterestedMessage")
                peer.peer_interested = true
            }
            (NotInterestedMessage().MSG_ID) -> {
                log("NotInterestedMessage")
                peer.peer_interested = false
            }
            (HaveMessage().MSG_ID) -> {

                val piece_index = HaveMessage().decode(peer.dataInputStream!!)
                Storage.setAvailable(piece_index, peer.ipAddr)
                log("HaveMessage: piece_index $piece_index")
            }
            (BitFieldMessage().MSG_ID) -> {

                val bitfield = BitFieldMessage().decode(peer.dataInputStream!!, len - 1)
                log("BitFieldMessage: $bitfield, bflen: ${bitfield.size}")
            }
            (RequestMessage().MSG_ID) -> {
                log("RequestMessage TODO")
            }
            (PieceMessage().MSG_ID) -> {
                log("PieceMessage TODO")
            }
            (CancelMessage().MSG_ID) -> {
                log("CancelMessage")
            }

            else -> log("Unknown Message Type received")
        }
    }

    private fun log(msg: String) = println("MessageReceiver for ${peer.ipAddr}: $msg")
}

class MessageSender(val peer: Peer) {

    fun sendMessage() {


        // 1 Send Interested, expect Unchocke
        if (peer.am_choking && !peer.am_interested && peer.peer_choking && !peer.peer_interested) {
            log("Sending InterestedMessage")
            peer.dataOutputStream!!.write(InterestedMessage().encode())

            // 2 Send Request, expect Piece
        } else if (peer.am_choking && !peer.am_interested && !peer.peer_choking && !peer.peer_interested) {

            log("Prepare RequestMessage ")
            val pieceToGet = Storage.getNextAvailable(peer.ipAddr)

            if (pieceToGet == null) {
                return
            }

            val msg = RequestMessage().encode(pieceIndex = pieceToGet, begin = 0)
            log("Sent RequestMessage: for piece $pieceToGet")
            peer.dataOutputStream!!.write(msg)
        } else {
//            log("Don't know shat to send  am_choking:$am_choking am_interested:$am_interested peer_choking:$peer_choking peer_interested:$peer_interested") //TODO
        }

    }

    private fun log(msg: String) = println("MessageSender for ${peer.ipAddr}: $msg")
}

data class HandShake(
        val pstrlen: Int = 19,
        val pstr: String = "BitTorrent protocol",
        val reserved: ByteArray = ByteArray(8),
        val infoHash: ByteArray,
        val peerId: String = PEER_ID
)

data class TrackerResponse(
        val failure_reason: String?,
        val warning_message: String?,
        val interval: Int?,
        val min_interval: Int?,
        val tracker_id: String?,
        val complete: Int?, // Seeders
        val incomplete: Int?, // leechers
        val peers: ByteArray? //TODO maybe list of maps
)

fun buildTrackerUrl(announce: String, totalTorrentSize: Int, infoHashed: ByteArray): String {


    val infoHashParam = byteArrayToURLString(infoHashed)

    println(infoHashParam)


    val b = URIBuilder(announce)
    b.addParameter("info_hash", infoHashParam)
    b.addParameter("peer_id", PEER_ID)
    b.addParameter("port", "6881")
    b.addParameter("uploaded", "0")
    b.addParameter("downloaded", "0")
    b.addParameter("left", totalTorrentSize.toString())
    b.addParameter("numwant", "80")
    b.addParameter("key", "60da50eb")
    b.addParameter("compact", "1")
    b.addParameter("event", "started")


    return b.build().toString().replace("""25""", "") //TODO remove replace
}

private fun hashInfoValue(info: ByteArray): ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    digest.reset()
    return digest.digest(info)

}


private fun byteArrayToURLString(input: ByteArray): String? {
    var ch: Byte = 0x00
    var i = 0
    if (input.size <= 0)
        return null

    val pseudo = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")
    val out = StringBuffer(input.size * 2)

    while (i < input.size) {
        // First check to see if we need ASCII or HEX
        if (input[i] >= '0'.toByte() && input[i] <= '9'.toByte()
                || input[i] >= 'a'.toByte() && input[i] <= 'z'.toByte()
                || input[i] >= 'A'.toByte() && input[i] <= 'Z'.toByte() || input[i] == '$'.toByte()
                || input[i] == '-'.toByte() || input[i] == '_'.toByte() || input[i] == '.'.toByte()
                || input[i] == '!'.toByte()) {
            out.append(input[i].toChar())
            i++
        } else {
            out.append('%')
            ch = (input[i].toInt() and 0xF0).toByte() // Strip off high nibble
            ch = ch.toInt().ushr(4).toByte() // shift the bits down
            ch = (ch.toInt() and 0x0F).toByte() // must do this if high order bit is on!
            out.append(pseudo[ch.toInt()]) // convert the nibble to a String Character
            ch = (input[i].toInt() and 0x0F).toByte() // Strip off low nibble
            out.append(pseudo[ch.toInt()]) // convert the nibble to a String Character
            i++
        }
    }

    val rslt = String(out)

    return rslt

}
