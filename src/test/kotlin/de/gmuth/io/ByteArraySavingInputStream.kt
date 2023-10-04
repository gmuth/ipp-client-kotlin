package de.gmuth.io

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import java.io.ByteArrayOutputStream
import java.io.InputStream

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