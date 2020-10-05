package de.gmuth.io

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class ByteArraySavingInputStream(val inputStream: InputStream) : InputStream() {

    private val byteArrayOutputStream = ByteArrayOutputStream()

    override fun read(): Int {
        val byte = inputStream.read()
        if (byte != -1) byteArrayOutputStream.write(byte)
        return byte
    }

    override fun close() {
        super.close()
        byteArrayOutputStream.close()
    }

    fun toByteArray(): ByteArray = byteArrayOutputStream.toByteArray()

}
