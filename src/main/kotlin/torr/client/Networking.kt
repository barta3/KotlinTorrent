package torr.client

import be.adaxisoft.bencode.BDecoder
import main.kotlin.torr.client.TorrentDecoder
import nl.komponents.kovenant.task
import org.apache.http.client.utils.URIBuilder
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by adrian on 05.07.16.
 */


const val PEER_ID = "-KB1000-ce43pvhlofit"


fun connectToTracker(torrentFile: TorrentDecoder.TorrentFile, info: ByteArray) {

    val infoHash = hashInfoValue(info)

    val url = URL(buildTrackerUrl(torrentFile.announce, torrentFile.calculateTotalSize(), infoHash))
    val con = url.openConnection() as HttpURLConnection

    println("\nSending 'GET' request to Tracker URL : " + url.toURI())
    println("Response Code : " + con.responseCode)
    // TODO: Check if reponse = 200

    val peers = parseTrackerResponse(ins = con.inputStream, infoHash = infoHash)

    peers.pmap {
        startThreads(it.ip, it.port, it.infoHash)
    }

//    for(i in peers.indices) {
//        println("Peer Info from Tracker[${i+1} / ${peers.size}]: ${peers[i]}")
//        startThreads(peers[i].ip, peers[i].port, peers[i].infoHash)
//    }

}

private fun parseTrackerResponse(ins: InputStream, infoHash: ByteArray): MutableList<PeerResult> {
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
        return mutableListOf()
    }

    if (response.warning_message != null) {
        println("WARNING: ${response.warning_message}")
    }

    val peers = response.peers!!

    var value: Long = 0
    for (i in 0..peers.size - 1) {
        value = (value shl 8) + (peers[i].toInt() and 0xff)
    }

    val parsedPeers = mutableListOf<PeerResult>()
    for (i in peers.indices) {
        if (i % 6 != 0) continue

        val ipAddr = "${(peers[i].toInt() and 0xff)}.${(peers[i + 1].toInt() and 0xff)}.${(peers[i + 2].toInt() and 0xff)}.${(peers[i + 3].toInt() and 0xff)}"

        if (ipAddr == "178.195.191.249") continue // It's me

        val port1 = (peers[i + 4].toInt() and 0xff)
        val port2 = (peers[i + 5].toInt() and 0xff)

        // http://stackoverflow.com/questions/1026761/how-to-convert-a-byte-array-to-its-numeric-value-java
//        val port = (peers[i+4].toInt() and 0xFF shl 8) or (peers[i+5].toInt() and 0xFF)
        val port = (port1 * 256) + port2

        val pr = PeerResult(ip = ipAddr, port = port, infoHash = infoHash)
        parsedPeers.add(pr)
    }

    return parsedPeers
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

        if (!handshake(infoHash)) {
            throw IllegalStateException("Infohash Check on handshake invalid")
        }

        // 1. Send Interested
        MessageSender(this).sendInteresed()
    }

    private fun handshake(infoHash: ByteArray): Boolean {

        dataOutputStream!!.write(HandshakeMessage().encode(HandShake(infoHash = infoHash)))
        val handShakeResult = HandshakeMessage().decode(dataInputStream!!)

        log("Infohash Check: : ${Arrays.equals(infoHash, handShakeResult.infoHash)}")

        return Arrays.equals(infoHash, handShakeResult.infoHash)
    }

    private fun log(msg: String) = println("Peer ${ipAddr}: $msg")

}


class MessageSender(val peer: Peer) {

    fun sendInteresed() {
        // Send Interested, expect Unchocke
        log("Sending InterestedMessage")
        peer.dataOutputStream!!.write(InterestedMessage().encode())
    }

    fun sendRequest() {

        // 2 Send Request, expect Piece

        log("Prepare RequestMessage ")
        val pieceToGet = Storage.getNextAvailable(peer.ipAddr)

        if (pieceToGet == null) {
            return
        }

        val msg = RequestMessage().encode(pieceIndex = pieceToGet, begin = 0)
        log("Sent RequestMessage: for piece $pieceToGet")
        peer.dataOutputStream!!.write(msg)


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

private fun startThreads(ipAddr: String, port: Int, infoHash: ByteArray) {

    // TODO: send / receive only after successfull connect

    val init = task {
        println("TASK Setup start")
        val peer = Peer(ipAddr)
        try {
            peer.setUpConnection(ipAddr, port, infoHash)
        } catch (c: ConnectException) {
            c.printStackTrace()
        }

        println("TASK Setup end")
        peer
    } fail {
        println("FAIL")
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

//    task {
//        while (true) {
////                println("SEND")
//            TimeUnit.SECONDS.sleep(1)
//            MessageSender(peer).sendMessage()
//        }
//    }

}


// TODO: move
fun <T, R> Iterable<T>.pmap(
        numThreads: Int = Runtime.getRuntime().availableProcessors() - 2,
        exec: ExecutorService = Executors.newFixedThreadPool(numThreads*4),
        transform: (T) -> R): List<R> {

    // default size is just an inlined version of kotlin.collections.collectionSizeOrDefault
    val defaultSize = if (this is Collection<*>) this.size else 10
    println("SIZE: $defaultSize")
    val destination = Collections.synchronizedList(ArrayList<R>(defaultSize*4))

    for (item in this) {
        exec.submit { destination.add(transform(item)) }
    }

    exec.shutdown()
    exec.awaitTermination(1, TimeUnit.DAYS)

    return ArrayList<R>(destination)
}
