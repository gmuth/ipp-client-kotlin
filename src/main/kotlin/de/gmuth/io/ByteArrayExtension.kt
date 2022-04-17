package de.gmuth.io

import java.io.ByteArrayOutputStream
import java.io.OutputStream

fun ByteArray.hexdump(maxRows: Int = 32, dump: (String) -> Unit) {
    val hexStringBuilder = StringBuilder()
    val charStringBuilder = StringBuilder()
    fun dumpLine() = dump("%-${maxRows * 3}s  '%s'".format(hexStringBuilder, charStringBuilder))
    for ((index, b) in withIndex()) {
        hexStringBuilder.append("%02X ".format(b))
        charStringBuilder.append(b.toInt().toChar())
        if ((index + 1) % maxRows == 0) {
            dumpLine()
            hexStringBuilder.clear()
            charStringBuilder.clear()
        }
    }
    if (isNotEmpty()) dumpLine()
}

fun ByteArray(writeBytes: (OutputStream) -> Unit): ByteArray =
    ByteArrayOutputStream().also { writeBytes(it) }.toByteArray()