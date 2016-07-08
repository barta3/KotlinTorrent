package torr.client

/**
 * Created by adrian on 08.07.16.
 */


fun main(args: Array<String>) {

    val s = "-DE13C0-DV5fhrr)eo1!"

    data class S(val s: ByteArray)

    println(s)
    val s1 = S(s.toByteArray())
    println(s1)

    println(s1.s.size)
}
