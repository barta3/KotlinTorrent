package torr.client

import main.kotlin.torr.client.TorrentDecoder
import org.apache.http.client.utils.URIBuilder
import java.io.DataInputStream
import java.io.DataOutputStream
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

class PeerHandler(val ipAddr: String, val port: Int, val infoHash: ByteArray) : Runnable {

    override fun run() {
        handShakeWithPeer(ipAddr, port, infoHash)
    }
}

fun handShakeWithPeer(peerIp: String, peerPort: Int, infoHash: ByteArray) {
    // handshake: <pstrlen><pstr><reserved><info_hash><peer_id>

    println("Handshake with $peerIp : $peerPort")

    (Socket(peerIp, peerPort)).use {

        println("connected")

        val input = DataInputStream(it.inputStream)
        val output = DataOutputStream(it.outputStream)

        val handShakeMsg = HandshakeMessage().encode(HandShake(19, "BitTorrent protocol", ByteArray(8), infoHash, PEER_ID))
        output.write(handShakeMsg)

        println(output.size())


        println("Decoding handshake from $peerIp")
        val hres = HandshakeMessage().decode(input)

        println("Handshake Response from $peerIp: $hres")
        println("Infohash Check: : ${Arrays.equals(infoHash , hres.infoHash)}")

        if(!Arrays.equals(infoHash , hres.infoHash)) {
            throw IllegalStateException("Infohash Check not OK!")
        }

    }

}

data class HandShake(
        val pstrlen: Int,
        val pstr: String,
        val reserved: ByteArray,
        val infoHash: ByteArray,
        val peerId: String
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
