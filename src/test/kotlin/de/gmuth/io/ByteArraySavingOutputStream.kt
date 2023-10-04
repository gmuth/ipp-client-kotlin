package de.gmuth.io

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import java.io.ByteArrayOutputStream
import java.io.OutputStream

class ByteArraySavingOutputStream(private val outputStream: OutputStream) : OutputStream() {

    var saveBytes: Boolean = true
    private val byteArrayOutputStream = ByteArrayOutputStream()

    override fun write(byte: Int) = outputStream.write(byte).also {
        if (saveBytes) byteArrayOutputStream.write(byte)
    }

    override fun close() {
        super.close()
        byteArrayOutputStream.close()
    }

    fun toByteArray(): ByteArray = byteArrayOutputStream.toByteArray()

}