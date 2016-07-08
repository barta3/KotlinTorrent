package torr.client

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

/**
 * Created by adrian on 06.07.16.
 */
class NetworkingKtTest {
    @Test
    fun buildTrackerUrl() {


        val path = Paths.get("/home/adrian/IdeaProjects/BittorrentClient/src/test/resources/info.b")
        val infoBlockInBytes = Files.readAllBytes(path)


        assertEquals(52308, infoBlockInBytes.size)

        val digest = MessageDigest.getInstance("SHA-1")
        digest.reset()
        val infoHash = digest.digest(infoBlockInBytes)

        val result = torr.client.buildTrackerUrl("http://bt1.archive.org:6969/announce", 10000, infoHash)

        val expected = "http://bt1.archive.org:6969/announce?info_hash=%F1%81%C63%5DMN6%8B%D49%D190%3D%26%DA%AEL%D9&peer_id=-KB1000-ce43pvhlofit&port=6881&uploaded=0&downloaded=0&left=10000&numwant=80&key=60da50eb&compact=1&event=started"

        assertEquals(expected, result)
    }

}