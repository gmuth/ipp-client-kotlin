package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ext.toPluralString
import de.gmuth.io.ByteArraySavingInputStream
import de.gmuth.io.ByteArraySavingOutputStream
import java.io.*
import java.nio.charset.Charset

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

    fun getSingleAttributesGroup(tag: IppTag): IppAttributesGroup {
        val groups = getAttributesGroups(tag)
        if (groups.isEmpty()) {
            throw IppException("no group found with tag '$tag' in $attributesGroups")
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

    val attributesCharset: Charset
        get() = (operationGroup["attributes-charset"] ?: throw IppException("missing 'attributes-charset'"))
                .value as Charset

    // --- DECODING ---

    fun read(inputStream: InputStream) {
        if (storeRawBytes) {
            val byteArraySavingInputStream = ByteArraySavingInputStream(inputStream)
            IppInputStream(byteArraySavingInputStream).readMessage(this)
            rawBytes = byteArraySavingInputStream.toByteArray()
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
            "%s: %s, %s: %s%s",
            javaClass.simpleName,
            codeDescription,
            attributesGroups.size.toPluralString("attributes group"),
            attributesGroups.map { it.tag },
            if (rawBytes == null) "" else ", ${rawBytes!!.size} raw bytes"
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