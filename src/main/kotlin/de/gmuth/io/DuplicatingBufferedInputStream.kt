package de.gmuth.io

import de.gmuth.log.Logging
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Copyright (c) 2021 Gerhard Muth
 */

open class DuplicatingBufferedInputStream(val inputStream: InputStream, private val outputStream: OutputStream) : BufferedInputStream(inputStream) {

    var duplicateBytes: Boolean = true
    protected var byteCount = 0

    companion object {
        val log = Logging.getLogger { }
    }

    override fun read(): Int {
        val byte = super.read()
        if (byte != -1) { // End of stream
            byteCount++
            log.trace { "read() @%-3d 0x%02X '%c'".format(byteCount, byte, byte.toChar()) }
            if (duplicateBytes) outputStream.write(byte)
        }
        return byte
    }

    override fun read(byteArray: ByteArray, offset: Int, length: Int): Int {
        val n = super.read(byteArray, offset, length)
        if (n > 0) {
            byteCount += n
            val newBytes = byteArray.copyOfRange(offset, offset + n)
            log.trace { "read() @%-3d '%s'".format(byteCount, String(newBytes)) }
            outputStream.write(newBytes, 0, n)
        }
        return n
    }

    override fun close() {
        super.close()
        outputStream.close()
    }

}