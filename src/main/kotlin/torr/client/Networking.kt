package torr.client

import be.adaxisoft.bencode.BEncodedValue
import be.adaxisoft.bencode.BEncoder
import main.kotlin.torr.client.TorrentDecoder
import org.apache.http.client.utils.URIBuilder
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * Created by adrian on 05.07.16.
 */


fun connectToTracker(torrentFile: TorrentDecoder.TorrentFile, info: ByteArray) {



//    val obj = URL(torrentFile.announce+"?")
//    val url = URL("http://www.google.com/search?q=mkyong")

    val b = URIBuilder(torrentFile.announce)
//    b.addParameter("t", "search")


    println(info.size)

    val digest = MessageDigest.getInstance("SHA-1")
    digest.reset()
    val infoHash = digest.digest(info)


    println(infoHash)
//    crypt.reset()
//    crypt.update(torrentFile)
//    b.addParameter("info_hash",
    val infoParam1 = byteArrayToURLString(infoHash)
    val infoParam2 = "n%C8%5Eqpg%EB%E1%BC_%CF%2C%01%11%D2%80%0D%85%A8%0D"
    val infoParam3 = URLEncoder.encode(String(infoHash), "UTF-8")
    val infoParam4 = "%5BB%40e9e54c2"

    println(infoParam1)
    println(infoParam2)
    println(infoParam3)
    println(infoParam4)

    b.addParameter("info_hash", infoParam2)
    b.addParameter("peer_id", "-TR2840-ce43pvhoofit")
    b.addParameter("port", "51413")
    b.addParameter("uploaded", "0")
    b.addParameter("downloaded", "0")
    b.addParameter("left", "103488585") //TODO
    b.addParameter("numwant", "80")
    b.addParameter("key", "60da49eb")
    b.addParameter("compact", "1")
//    b.addParameter("event", "started")


    val urls = b.build().toString().replace("""25""", "")
    val url  = URL(urls)

    val con = url.openConnection() as HttpURLConnection

    // optional default is GET
    con.requestMethod = "GET"


    //add request header
//    con.setRequestProperty("User-Agent", USER_AGENT)



    println("\nSending 'GET' request to URL : " + url.toURI())
    println("Response Code : " + con.responseCode)


    val ins = BufferedReader(InputStreamReader(con.getInputStream()))
    var inputLine: String
    val response = StringBuffer()

    print(ins.readLine())

//    while ((inputLine = ins.readLine()) != null) {
//        response.append(inputLine)
//    }
    ins.close()

    //print result
//    println(response.toString())

}

fun byteArrayToURLString(input: ByteArray): String? {
    var ch: Byte = 0x00
    var i = 0
    if (input == null || input.size <= 0)
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
            ch = (ch.toInt() and 0x0F).toByte() // must do this is high order bit is
            // on!
            out.append(pseudo[ch.toInt()]) // convert the nibble to a
            // String Character
            ch = (input[i].toInt() and 0x0F).toByte() // Strip off low nibble
            out.append(pseudo[ch.toInt()]) // convert the nibble to a
            // String Character
            i++
        }
    }

    val rslt = String(out)

    return rslt

}
