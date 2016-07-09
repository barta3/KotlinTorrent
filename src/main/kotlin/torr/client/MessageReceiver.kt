package torr.client

import java.io.EOFException
import java.util.concurrent.TimeUnit

/**
 * Created by adrian on 09.07.16.
 */

class MessageReceiver(val peer: Peer) {

    val LOG by lazyLogger()

    fun decodeIncomingMessage() {

        var len = -1


        while(len < 0) {
            TimeUnit.MILLISECONDS.sleep(500)
            try {
                len = peer.dataInputStream!!.readInt()
            } catch (eof : EOFException) {
                // No Data available
            }
        }

        if(len > 100) println("!!BIG MSG")


        if (len == 0) {
            log("Keepalive")
            return
        }

        var msgType: Byte = (-1).toByte()

        while(msgType < 0.toByte()) {
            TimeUnit.MILLISECONDS.sleep(500)
            try {
                msgType = peer.dataInputStream!!.readByte()
            } catch (eof: EOFException) {
                // No Data
            }
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
                log("HaveMessage: piece_index $piece_index")
                Storage.setAvailable(piece_index, peer.ipAddr)
                MessageSender(peer).sendRequest()
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

    private fun log(msg: String) = LOG.info("MessageReceiver for ${peer.ipAddr}: $msg")
}