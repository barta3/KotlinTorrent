package torr.client

import be.adaxisoft.bencode.BDecoder
import main.kotlin.torr.client.TorrentDecoder
import org.apache.http.client.utils.URIBuilder
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Created by adrian on 09.07.16.
 */

class Tracker {
    val LOG by lazyLogger()

    fun connectToTracker(torrentFile: TorrentDecoder.TorrentFile, info: ByteArray) {

        val infoHash = hashInfoValue(info)

        val url = URL(buildTrackerUrl(torrentFile.announce, torrentFile.calculateTotalSize(), infoHash))
        val con = url.openConnection() as HttpURLConnection

        LOG.info("\nSending 'GET' request to Tracker URL : " + url.toURI())
        LOG.info("Response Code : " + con.responseCode)
        // TODO: Check if reponse = 200

        val peers = parseTrackerResponse(ins = con.inputStream, infoHash = infoHash)

        peers.pmap {
            startThreads(it.ip, it.port, it.infoHash)
        }

    }

    private fun parseTrackerResponse(ins: InputStream, infoHash: ByteArray): MutableList<PeerResult> {
        val map = BDecoder(ins).decodeMap().map
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

        LOG.info("Tracker response: " + response)

        if (response.failure_reason != null) {
            LOG.severe("FAILED: ${response.failure_reason}")
            return mutableListOf()
        }

        if (response.warning_message != null) {
            LOG.warning("WARNING: ${response.warning_message}")
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

    private fun hashInfoValue(info: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.reset()
        return digest.digest(info)
    }


    fun buildTrackerUrl(announce: String, totalTorrentSize: Int, infoHashed: ByteArray): String {


        val infoHashParam = byteArrayToURLString(infoHashed)

        LOG.info(infoHashParam.toString())


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
}