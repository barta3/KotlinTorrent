package torr.client

import be.adaxisoft.bencode.BDecoder
import be.adaxisoft.bencode.BEncoder
import main.kotlin.torr.client.TorrentDecoder
import java.io.File
import java.io.FileInputStream
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.companionObject

/**
 * Created by adrian on 05.07.16.
 */

fun main(args: Array<String>) {

    Client().statrt()
}

class Client {

    val LOG by lazyLogger()

    fun statrt() {

        LOG.info("Starting Client")

        val fileName = "/home/adrian/Downloads/Enty.torrent"
//    val fileName = "/home/adrian/IdeaProjects/BittorrentClient/src/test/resources/1999-04-16.paf.sbd.unknown.10169.sbeok.flacf_archive.torrent"
//    val fileName = "/home/adrian/Downloads/wikimediacommons-200508_archive.torrent"
        val file = File(fileName)
        val torrent = TorrentDecoder().decode(file)

        LOG.info("Starting Client")

        val info = BDecoder(FileInputStream(file)).decodeMap().map["info"]!!.map
        val bb = BEncoder.encode(info)
        val bytes = ByteArray(bb.remaining())
        bb.get(bytes)


//    val path = Paths.get("/home/adrian/IdeaProjects/BittorrentClient/src/test/resources/info.b")
//    Files.write(path, bytes)

        Tracker().connectToTracker(torrent, bytes)

        Storage.init(torrent)
    }

}



/**
 * Logging based on http://stackoverflow.com/questions/34416869/idiomatic-way-of-logging-in-kotlin
*/

// Return logger for Java class, if companion object fix the name
public fun <T: Any> logger(forClass: Class<T>): Logger {
    return Logger.getLogger(unwrapCompanionClass(forClass).name)
}

// unwrap companion class to enclosing class given a Java Class
public fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }
}

// unwrap companion class to enclosing class given a Kotlin Class
public fun <T: Any> unwrapCompanionClass(ofClass: KClass<T>): KClass<*> {
    return unwrapCompanionClass(ofClass.java).kotlin
}

// Return logger for Kotlin class
public fun <T: Any> logger(forClass: KClass<T>): Logger {
    return logger(forClass.java)
}

// return logger from extended class (or the enclosing class)
public fun <T: Any> T.logger(): Logger {
    return logger(this.javaClass)
}

// return a lazy logger property delegate for enclosing class
public fun <R : Any> R.lazyLogger(): Lazy<Logger> {
    return lazy { logger(this.javaClass) }
}

// return a logger property delegate for enclosing class
public fun <R : Any> R.injectLogger(): Lazy<Logger> {
    return lazyOf(logger(this.javaClass))
}

// marker interface and related extension (remove extension for Any.logger() in favour of this)
interface Loggable {}
public fun Loggable.logger(): Logger = logger(this.javaClass)

// abstract base class to provide logging, intended for companion objects more than classes but works for either
public abstract class WithLogging: Loggable {
    val LOG = logger()
}

