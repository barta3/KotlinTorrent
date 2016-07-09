package torr.client

import nl.komponents.kovenant.task
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ConnectException
import java.net.Socket
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by adrian on 05.07.16.
 */


const val PEER_ID = "-KB1000-ce43pvhlofit"


class Peer(val ipAddr: String) { //TODO: change to id

    val LOG by lazyLogger()

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
        MessageSender(this).sendInterested()
    }

    private fun handshake(infoHash: ByteArray): Boolean {

        dataOutputStream!!.write(HandshakeMessage().encode(HandShake(infoHash = infoHash)))
        val handShakeResult = HandshakeMessage().decode(dataInputStream!!)

        log("Infohash Check: : ${Arrays.equals(infoHash, handShakeResult.infoHash)}")

        return Arrays.equals(infoHash, handShakeResult.infoHash)
    }

    private fun log(msg: String) = LOG.info("Peer ${ipAddr}: $msg")

}


class MessageSender(val peer: Peer) {

    val LOG by lazyLogger()

    fun sendInterested() {
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


        Storage.setRequested(pieceToGet, peer.ipAddr)

    }

    private fun log(msg: String) = LOG.info("MessageSender for ${peer.ipAddr}: $msg")
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


fun startThreads(ipAddr: String, port: Int, infoHash: ByteArray) {

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
            TimeUnit.MILLISECONDS.sleep(1000)
            MessageReceiver(peer).decodeIncomingMessage()
        }
    }

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
