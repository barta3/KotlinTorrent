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
        println("Storage setAvailable piece $id peerId $peerId")
        pieces[id].state = PieceState.AVAILABLE
        pieces[id].peerIp = peerId
    }

}

data class Piece(val id: Int,
                 var state: PieceState,
                 val data: ByteArray?,
                 var peerIp: String = ""
)

enum class PieceState {
    TODO, REQUESTED, AVAILABLE, COMPLETE,
}