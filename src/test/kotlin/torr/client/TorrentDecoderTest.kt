package torr.client

import main.kotlin.torr.client.TorrentDecoder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Created by adrian on 05.07.16.
 */
class TorrentDecoderTest {
    @Test
    fun decode() {

        val classLoader = javaClass.classLoader
        val file = File(classLoader.getResource("1999-04-16.paf.sbd.unknown.10169.sbeok.flacf_archive.torrent")!!.file)

        val result = TorrentDecoder().decode(file)

        assertEquals("http://bt1.archive.org:6969/announce", result.announce)

        val expectedComment = """
        |This content hosted at the Internet Archive at https://archive.org/details/1999-04-16.paf.sbd.unknown.10169.sbeok.flacf
        |Files may have changed, which prevents torrents from downloading correctly or completely; please check for an updated torrent at https://archive.org/download/1999-04-16.paf.sbd.unknown.10169.sbeok.flacf/1999-04-16.paf.sbd.unknown.10169.sbeok.flacf_archive.torrent
        |Note: retrieval usually requires a client that supports webseeding (GetRight style).
        |Note: many Internet Archive torrents contain a 'pad file' directory. This directory and the files within it may be erased once retrieval completes.
        |Note: the file 1999-04-16.paf.sbd.unknown.10169.sbeok.flacf_meta.xml contains metadata about this torrent's contents.""".trimMargin()

        assertEquals(expectedComment, result.comment)
        assertEquals(1465563615, result.creation_date)


        assertEquals(142, result.info.files.size)
        assertEquals(1402626825, result.calculateTotalSize()) // ~1.4 GB

        assertEquals(2802, result.info.files[0].length)
        assertEquals(listOf("1999-04-16.paf.sbd.unknown.10169.sbeok.flacf_meta.xml"), result.info.files[0].path)

        assertEquals(32437, result.info.files[18].length)
        assertEquals(listOf("pf99-04-16d1t02_esslow.json.gz"), result.info.files[18].path)

        assertEquals("1999-04-16.paf.sbd.unknown.10169.sbeok.flacf", result.info.name)
        assertEquals(1048576, result.info.piece_length)
        assertEquals(25271, result.info.pieces.length)
    }

}