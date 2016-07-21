package main.kotlin.torr.client

import be.adaxisoft.bencode.BDecoder
import java.io.File
import java.io.FileInputStream

/**
 * Created by adrian on 05.07.16.
 */

class TorrentDecoder {

    fun decode(file: File): TorrentFile {
        val decoder = BDecoder(FileInputStream(file))
        val document = decoder.decodeMap()


        val announce = document.map["announce"]!!.string

        val comment = document.map["comment"]!!.string
        val creation_date = document.map["creation date"]!!.number.toInt()

        val oInfo = document.map["info"]!!.map

        val info = TorrentInfo(
                piece_length = oInfo["piece length"]!!.int,
                pieces = String(oInfo["pieces"]!!.bytes),
                name = oInfo["name"]!!.string,
                files = mutableListOf<InfoFile>()
        )
        val files = oInfo["files"]!!.list

        for (file in files) {
            info.files.add(InfoFile(
                    length = file.map["length"]!!.int,
                    path = file.map["path"]!!.list.map { it -> it.string }
            ))
        }


        val torrentFile = TorrentFile(announce.toString(), comment, creation_date, info)

        return torrentFile
    }

    data class TorrentFile(
            val announce: String,
            val comment: String,
            val creation_date: Int,
            val info: TorrentInfo
    ) {
        fun calculateTotalSize() =  info.files.sumBy { it.length }

        fun getFileByPieceIndex(pieceIndex : Int) : File {
            val r = pieceIndex * 16384 // blocksize

            val path = info.files.takeWhile { info.files.sumBy { it.length } < r }.first().path

            println("piece Inde: $pieceIndex / File to write: $path")
            return File(path[0])
        }
    }

    data class TorrentInfo(
            val piece_length: Int,
            val pieces: String,
            val name: String,
            val files: MutableList<InfoFile>
    )

    data class InfoFile(
            val length: Int,
            val path: List<String>
    )
}

fun main(args: Array<String>) {
    print(TorrentDecoder().decode(File("""/home/adrian/Downloads/1999-04-16.paf.sbd.unknown.10169.sbeok.flacf_archive.torrent""")))

}
