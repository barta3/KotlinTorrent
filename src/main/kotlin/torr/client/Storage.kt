package torr.client

import main.kotlin.torr.client.TorrentDecoder
import nl.komponents.kovenant.task

/**
 * Created by adrian on 08.07.16.
 */

object Storage {

    private val pieces = Array<StoragePiece>(100, { i -> StoragePiece(i, PieceState.TODO, null) })
    private var t : TorrentDecoder.TorrentFile? = null

    fun getNextAvailable(peerIp: String): Int? {

        val req = pieces.find { it.state == PieceState.AVAILABLE && it.peerIp == peerIp }?.id

        if(req == null) {
            println("NOTHING FOUND for peer $peerIp")
            return null
        }

        println("Storage getNextAvailable  for Peer $peerIp , found: $req")

        pieces[req].state = PieceState.REQUESTED
        return req
    }

    fun setAvailable(id: Int, peerId: String) {
        setPieceTo(id, PieceState.AVAILABLE, peerId)
    }

    fun setRequested(id: Int, peerId: String) {
        setPieceTo(id, PieceState.REQUESTED, peerId)
    }

    fun setDownloaded(id: Int, peerId: String, data: ByteArray?) {
        setPieceTo(id, PieceState.DOWNLOADED, peerId)
        pieces[id].data = data

        task {
            // write file
            // 1. find path from decoded torrent
            // 2. write bytes with correct offset
            println("Searching for File for piece $id")
            val f = t!!.getFileByPieceIndex(id)
        }
    }



    private fun setPieceTo(pieceId: Int, state: PieceState, peerId: String) {
        println("Storage piece $pieceId to $state for $peerId")
        pieces[pieceId].state = state
        pieces[pieceId].peerIp = peerId
    }

    fun  init(torrent: TorrentDecoder.TorrentFile) {
        t = torrent
    }

}

data class StoragePiece(val id: Int,
                 var state: PieceState,
                 var data: ByteArray?,
                 var peerIp: String = ""
)

enum class PieceState {
    TODO, REQUESTED, AVAILABLE, DOWNLOADED, FILE
}