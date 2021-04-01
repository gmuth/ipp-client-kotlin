package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.io.ByteArraySavingInputStream
import de.gmuth.io.ByteArraySavingOutputStream
import de.gmuth.log.Logging
import java.io.*

abstract class IppMessage {

    var code: Short? = null
    var requestId: Int? = null
    var version: IppVersion? = null
    val attributesGroups = mutableListOf<IppAttributesGroup>()
    var documentInputStream: InputStream? = null
    var documentInputStreamIsConsumed: Boolean = false
    var rawBytes: ByteArray? = null

    abstract val codeDescription: String // request operation or response status

    companion object {
        val log = Logging.getLogger {}
        var saveRawBytes: Boolean = true
    }

    val operationGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Operation)

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Job)

    fun getAttributesGroups(tag: IppTag) =
            attributesGroups.filter { it.tag == tag }

    fun getSingleAttributesGroup(tag: IppTag, createIfMissing: Boolean = false): IppAttributesGroup {
        val groups = getAttributesGroups(tag)
        if (groups.isEmpty()) {
            if (createIfMissing) {
                return createAttributesGroup(tag)
            } else {
                throw IppException("no group found with tag '$tag' in $attributesGroups")
            }
        }
        return groups.single()
    }

    fun containsGroup(tag: IppTag) =
            attributesGroups.map { it.tag }.contains(tag)

    // factory/build method for IppAttributesGroup
    fun createAttributesGroup(tag: IppTag, replacementAllowed: Boolean = true): IppAttributesGroup =
            IppAttributesGroup(tag, replacementAllowed).apply { attributesGroups.add(this) }

    // --------
    // ENCODING
    // --------

    fun write(outputStream: OutputStream) {
        if (saveRawBytes) {
            val byteArraySavingOutputStream = ByteArraySavingOutputStream(outputStream)
            try {
                IppOutputStream(byteArraySavingOutputStream).writeMessage(this)
            } finally {
                rawBytes = byteArraySavingOutputStream.toByteArray()
            }
        } else {
            IppOutputStream(outputStream).writeMessage(this)
        }
        copyDocument(outputStream)
    }

    fun write(file: File) =
            write(FileOutputStream(file))

    fun encode(): ByteArray =
            with(ByteArrayOutputStream()) {
                write(this)
                return toByteArray()
            }

    // --------
    // DECODING
    // --------

    fun read(inputStream: InputStream) {
        if (saveRawBytes) {
            val byteArraySavingInputStream = ByteArraySavingInputStream(inputStream)
            try {
                IppInputStream(byteArraySavingInputStream).readMessage(this)
            } finally {
                rawBytes = byteArraySavingInputStream.toByteArray()
            }
        } else {
            IppInputStream(inputStream).readMessage(this)
        }
        documentInputStream = inputStream
    }

    fun read(file: File) =
            read(FileInputStream(file))

    fun decode(byteArray: ByteArray) =
            read(ByteArrayInputStream(byteArray))

    // --------
    // DOCUMENT
    // --------

    private fun copyDocument(outputStream: OutputStream) {
        if (documentInputStreamIsConsumed) log.warn { "documentInputStream is consumed" }
        documentInputStream?.copyTo(outputStream)
        log.debug { "consumed documentInputStream" }
        documentInputStreamIsConsumed = true
    }

    fun saveDocument(file: File) {
        copyDocument(file.outputStream())
        log.info { "saved ${file.length()} document bytes to file ${file.path}" }
    }

    // -------
    // LOGGING
    // -------

    override fun toString(): String =
            String.format(
                    "%s %s%s",
                    codeDescription,
                    attributesGroups.map { "${it.values.size} ${it.tag}" },
                    if (rawBytes == null) "" else " (${rawBytes!!.size} bytes)"
            )

    fun logDetails(prefix: String = "") {
        if (rawBytes != null) log.info { "${prefix}${rawBytes!!.size} raw ipp bytes" }
        log.info { "${prefix}version = $version" }
        log.info { "${prefix}$codeDescription" }
        log.info { "${prefix}request-id = $requestId" }
        for (group in attributesGroups) {
            group.logDetails(prefix)
        }
    }

}