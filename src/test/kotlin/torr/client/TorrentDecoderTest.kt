package torr.client

import main.kotlin.torr.client.TorrentDecoder
import org.junit.Test

import org.junit.Assert.*
import java.io.File

/**
 * Created by adrian on 05.07.16.
 */
class TorrentDecoderTest {
    @Test
    fun decode() {

        val classLoader = javaClass.classLoader
        val file = File(classLoader.getResource("Multifile.torrent")!!.file)

        val result = TorrentDecoder().decode(file)

        assertEquals("http://tracker.mininova.org/announce", result.announce)
        assertEquals("Auto-generated torrent by Mininova.org CD", result.comment)
        assertEquals(1453201198, result.creation_date)


        assertEquals(19, result.info.files.size)

        assertEquals(7602896, result.info.files[0].length)
        assertEquals(listOf("01 Enty3way Finger Bang Me - Enty3way The God.mp3"), result.info.files[0].path)

        assertEquals(74101, result.info.files[18].length)
        assertEquals(listOf("Lyrics by Enty3way The God Delusions Of Grandeur.pdf"), result.info.files[18].path)

        assertEquals("Enty3way The God - Delusions of Grandeur", result.info.name)
        assertEquals(1048576, result.info.piece_length)
        assertEquals(1873, result.info.pieces.length)
    }

}