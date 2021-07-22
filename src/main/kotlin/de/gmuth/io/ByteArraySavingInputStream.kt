package de.gmuth.io

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

class ByteArraySavingInputStream(private val inputStream: InputStream) : InputStream() {

    var saveBytes: Boolean = true
    private val byteArrayOutputStream = ByteArrayOutputStream()

    override fun read() = inputStream.read().also {
        if (saveBytes && it != -1) byteArrayOutputStream.write(it)
    }

    override fun close() {
        super.close()
        byteArrayOutputStream.close()
    }

    fun toByteArray(): ByteArray = byteArrayOutputStream.toByteArray()

}