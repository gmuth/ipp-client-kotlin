package de.gmuth.io

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class FileSavingInputStream(val file: File, val inputStream: InputStream) : InputStream() {

    private val fileOutputStream = FileOutputStream(file)

    override fun read(): Int {
        val byte = inputStream.read()
        if (byte != -1) fileOutputStream.write(byte)
        return byte
    }

    override fun close() {
        super.close()
        fileOutputStream.close()
    }

}
