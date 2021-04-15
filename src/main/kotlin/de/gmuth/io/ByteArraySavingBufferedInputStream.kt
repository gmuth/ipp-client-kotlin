package de.gmuth.io

import de.gmuth.log.Logging
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Copyright (c) 2021 Gerhard Muth
 */

class ByteArraySavingBufferedInputStream(val inputStream: InputStream) : BufferedInputStream(inputStream) {

    var saveBytes: Boolean = true

    private val byteArrayOutputStream = ByteArrayOutputStream()
    private var byteCount = 0

    companion object {
        val log = Logging.getLogger { }
    }

    override fun read(): Int {
        val byte = super.read()
        if (byte != -1) { // End of stream
            byteCount++
            log.trace { "read() @%-3d 0x%02X '%c'".format(byteCount, byte, byte.toChar()) }
            if (saveBytes) byteArrayOutputStream.write(byte)
        }
        return byte
    }

    override fun read(byteArray: ByteArray, offset: Int, length: Int): Int {
        val n = super.read(byteArray, offset, length)
        if (n > 0) {
            byteCount += n
            val newBytes = byteArray.copyOfRange(offset, offset + n)
            log.trace { "read() @%-3d '%s'".format(byteCount, String(newBytes)) }
            byteArrayOutputStream.writeBytes(newBytes)
        }
        return n
    }

    override fun close() {
        super.close()
        byteArrayOutputStream.close()
    }

    fun toByteArray(): ByteArray {
        log.debug { "$byteCount bytes saved" }
        return byteArrayOutputStream.toByteArray()
    }

}