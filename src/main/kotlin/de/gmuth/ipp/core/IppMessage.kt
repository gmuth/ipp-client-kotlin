package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset

abstract class IppMessage {

    var version: IppVersion? = null
    var code: Short? = null
    abstract val codeDescription: String // request operation or response status
    var requestId: Int? = null
    val attributesGroups = mutableListOf<IppAttributesGroup>()

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

    fun readFrom(inputStream: InputStream) =
            IppInputStream(inputStream).readMessage(this)

    fun readFrom(file: File) =
            readFrom(FileInputStream(file))

    // --- ENCODING ---

    fun bytes(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        IppOutputStream(byteArrayOutputStream).writeMessage(this)
        return byteArrayOutputStream.toByteArray()
    }

    // --- LOGGING ---

    override fun toString(): String = String.format(
            "%s: %s, %s: %s",
            javaClass.simpleName,
            codeDescription,
            attributesGroups.size.toPluralString("attributes group"),
            attributesGroups.map { it.tag }
    )

    fun logDetails(prefix: String = "") {
        println("${prefix}version = $version")
        println("${prefix}$codeDescription")
        println("${prefix}request-id = $requestId")
        for (group in attributesGroups) {
            group.logDetails(prefix)
        }
    }

}