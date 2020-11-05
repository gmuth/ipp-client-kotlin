package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.io.ByteArraySavingInputStream
import de.gmuth.io.ByteArraySavingOutputStream
import java.io.*

abstract class IppMessage {

    var version: IppVersion? = null
    var code: Short? = null
    abstract val codeDescription: String // request operation or response status
    var requestId: Int? = null
    val attributesGroups = mutableListOf<IppAttributesGroup>()
    var rawBytes: ByteArray? = null

    companion object {
        var storeRawBytes: Boolean = true
    }

    fun getAttributesGroups(tag: IppTag) = attributesGroups.filter { it.tag == tag }

    fun getSingleAttributesGroup(tag: IppTag, createIfMissing: Boolean = false): IppAttributesGroup {
        val groups = getAttributesGroups(tag)
        if (groups.isEmpty()) {
            if (createIfMissing) {
                return ippAttributesGroup(tag)
            } else {
                throw IppException("no group found with tag '$tag' in $attributesGroups")
            }
        }
        return groups.single()
    }

    fun ippAttributesGroup(tag: IppTag): IppAttributesGroup {
        val group = IppAttributesGroup(tag)
        attributesGroups.add(group)
        return group
    }

    val operationGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Operation)

    // --- DECODING ---

    fun read(inputStream: InputStream) {
        if (storeRawBytes) {
            val byteArraySavingInputStream = ByteArraySavingInputStream(inputStream)
            try {
                IppInputStream(byteArraySavingInputStream).readMessage(this)
            } catch (exception: Exception) {
                // required to 'finally' save bytes
                throw exception
            } finally {
                rawBytes = byteArraySavingInputStream.toByteArray()
            }
        } else {
            IppInputStream(inputStream).readMessage(this)
        }
    }

    fun read(file: File) = read(FileInputStream(file))

    fun decode(byteArray: ByteArray) = read(ByteArrayInputStream(byteArray))

    // --- ENCODING ---

    open fun write(outputStream: OutputStream) {
        if (storeRawBytes) {
            val byteArraySavingOutputStream = ByteArraySavingOutputStream(outputStream)
            IppOutputStream(byteArraySavingOutputStream).writeMessage(this)
            rawBytes = byteArraySavingOutputStream.toByteArray()
        } else {
            IppOutputStream(outputStream).writeMessage(this)
        }
    }

    fun write(file: File) = write(FileOutputStream(file))

    fun encode(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        write(byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    // --- LOGGING ---

    override fun toString(): String = String.format(
            "%s %s%s",
            codeDescription,
            attributesGroups.map { "${it.values.size} ${it.tag}" },
            if (rawBytes == null) "" else " (${rawBytes!!.size} bytes)"
    )

    fun logDetails(prefix: String = "") {
        if (rawBytes != null) println("${prefix}${rawBytes!!.size} raw ipp bytes")
        println("${prefix}version = $version")
        println("${prefix}$codeDescription")
        println("${prefix}request-id = $requestId")
        for (group in attributesGroups) {
            group.logDetails(prefix)
        }
    }
}