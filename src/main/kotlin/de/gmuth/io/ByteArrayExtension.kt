package de.gmuth.io

import java.io.ByteArrayOutputStream
import java.io.OutputStream

fun ByteArray.hexdump(maxRows: Int = 32, dump: (String) -> Unit) =
        with(StringBuilder()) {
            for ((index, b) in withIndex()) {
                append("%02X ".format(b))
                if ((index + 1) % maxRows == 0) {
                    dump(toString())
                    clear()
                }
            }
            if (isNotEmpty()) dump(toString())
        }

fun ByteArray(writeContent: (OutputStream) -> Unit): ByteArray =
    ByteArrayOutputStream().also { writeContent(it) }.toByteArray()