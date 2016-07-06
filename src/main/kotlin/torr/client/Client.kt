package torr.client

import be.adaxisoft.bencode.BDecoder
import be.adaxisoft.bencode.BEncodedValue
import be.adaxisoft.bencode.BEncoder
import main.kotlin.torr.client.TorrentDecoder
import java.io.File
import java.io.FileInputStream

/**
 * Created by adrian on 05.07.16.
 */
fun main(args: Array<String>) {
    val file = File("""/home/adrian/Downloads/Enty.torrent""")
    val torrent = TorrentDecoder().decode(file)

    //
    val info = BDecoder(FileInputStream(file)).decodeMap().map["info"]!!.map
    val bb = BEncoder.encode(info)
    val bytes = ByteArray(bb.remaining())
    bb.get(bytes)

//
//    val info = torrent.info.toB
//
    connectToTracker(torrent, bytes)

}