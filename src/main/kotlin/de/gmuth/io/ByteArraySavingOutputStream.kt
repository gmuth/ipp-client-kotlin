package de.gmuth.io

import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class ByteArraySavingOutputStream(val outputStream: OutputStream) : OutputStream() {

    private val byteArrayOutputStream = ByteArrayOutputStream()

    override fun write(byte: Int) {
        outputStream.write(byte)
        byteArrayOutputStream.write(byte)
    }

    override fun close() {
        super.close()
        byteArrayOutputStream.close()
    }

    fun toByteArray(): ByteArray = byteArrayOutputStream.toByteArray()

}
