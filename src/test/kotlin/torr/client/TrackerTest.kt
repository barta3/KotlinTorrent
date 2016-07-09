package torr.client

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Created by adrian on 06.07.16.
 */
class TrackerTest {
    @Test
    fun buildTrackerUrlT() {


        val classLoader = javaClass.classLoader
        val file = File(classLoader.getResource("info.b")!!.file)
        val infoBlockInBytes = Files.readAllBytes(file.toPath())

        assertEquals(52308, infoBlockInBytes.size)

        val digest = MessageDigest.getInstance("SHA-1")
        digest.reset()
        val infoHash = digest.digest(infoBlockInBytes)

        val result = Tracker().buildTrackerUrl("http://bt1.archive.org:6969/announce", 10000, infoHash)

        val expected = "http://bt1.archive.org:6969/announce?info_hash=%F1%81%C63%5DMN6%8B%D49%D190%3D%26%DA%AEL%D9&peer_id=-KB1000-ce43pvhlofit&port=6881&uploaded=0&downloaded=0&left=10000&numwant=80&key=60da50eb&compact=1&event=started"

        assertEquals(expected, result)
    }

}