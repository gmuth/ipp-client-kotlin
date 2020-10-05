package de.gmuth.io

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class FileSavingOutputStream(val file: File, val outputStream: OutputStream) : OutputStream() {

    private val fileOutputStream = FileOutputStream(file)

    override fun write(byte: Int) {
        outputStream.write(byte)
        fileOutputStream.write(byte)
    }

    override fun close() {
        super.close()
        fileOutputStream.close()
    }

}
