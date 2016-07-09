package torr.client

/**
 * Created by adrian on 08.07.16.
 */

object Storage {


    private val pieces = Array<Piece>(100, { i -> Piece(i, PieceState.TODO, null) })

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

    private fun setPieceTo(pieceId: Int, state: PieceState, peerId: String) {
        println("Storage piece $pieceId to $state for $peerId")
        pieces[pieceId].state = state
        pieces[pieceId].peerIp = peerId
    }

}

data class Piece(val id: Int,
                 var state: PieceState,
                 val data: ByteArray?,
                 var peerIp: String = ""
)

enum class PieceState {
    TODO, REQUESTED, AVAILABLE, DOWNLOADING, COMPLETE,
}