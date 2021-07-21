package de.gmuth.io

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Copyright (c) 2021 Gerhard Muth
 */

class ByteArraySavingBufferedInputStream(
        inputStream: InputStream,
        private val byteArrayOutputStream: ByteArrayOutputStream = ByteArrayOutputStream()

) : DuplicatingBufferedInputStream(inputStream, byteArrayOutputStream) {

    fun toByteArray(): ByteArray {
        log.debug { "$byteCount bytes saved" }
        return byteArrayOutputStream.toByteArray()
    }

}