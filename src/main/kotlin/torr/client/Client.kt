package torr.client

import be.adaxisoft.bencode.BDecoder
import be.adaxisoft.bencode.BEncoder
import main.kotlin.torr.client.TorrentDecoder
import java.io.File
import java.io.FileInputStream

/**
 * Created by adrian on 05.07.16.
 */

fun main(args: Array<String>) {

    val fileName = "/home/adrian/Downloads/Enty.torrent"
//    val fileName = "/home/adrian/IdeaProjects/BittorrentClient/src/test/resources/1999-04-16.paf.sbd.unknown.10169.sbeok.flacf_archive.torrent"
//    val fileName = "/home/adrian/Downloads/wikimediacommons-200508_archive.torrent"
    val file = File(fileName)
    val torrent = TorrentDecoder().decode(file)


    val info = BDecoder(FileInputStream(file)).decodeMap().map["info"]!!.map
    val bb = BEncoder.encode(info)
    val bytes = ByteArray(bb.remaining())
    bb.get(bytes)


//    val path = Paths.get("/home/adrian/IdeaProjects/BittorrentClient/src/test/resources/info.b")
//    Files.write(path, bytes)

    connectToTracker(torrent, bytes)

}