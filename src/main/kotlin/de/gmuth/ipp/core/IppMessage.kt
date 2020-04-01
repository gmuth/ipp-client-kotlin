package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

abstract class IppMessage {

    var version: IppVersion? = null
    var requestId: Int? = null
    var attributesCharset: Charset? = null

    protected var code: Short? = null
    abstract val codeDescription: String
    private val attributesGroups = mutableListOf<IppAttributesGroup>()

    fun newAttributesGroup(tag: IppTag): IppAttributesGroup {
        val group = IppAttributesGroup(tag)
        attributesGroups.add(group)
        return group
    }

    fun getSingleAttributesGroup(tag: IppTag) = attributesGroups.filter { it.tag == tag }.single()

    // --------------------------------------------------------------------- ENCODING

    private fun writeTo(outputStream: OutputStream) {
        if (version == null) throw IllegalArgumentException("version must not be null")
        if (code == null) throw IllegalArgumentException("code must not be null")
        if (requestId == null) throw IllegalArgumentException("requestId must not be null")
        if (attributesCharset == null) throw IllegalArgumentException("attributesCharset must not be null")

        with(IppOutputStream(outputStream, attributesCharset as Charset)) {
            writeVersion(version as IppVersion)
            writeCode(code as Short)
            writeRequestId(requestId as Int)
            attributesGroups.forEach { writeAttributesGroup(it) }
            writeTag(IppTag.End)
            close()
        }
    }

    fun toByteArray(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        writeTo(byteArrayOutputStream)
        byteArrayOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun toInputStream(): InputStream {
        return ByteArrayInputStream(toByteArray())
    }

    // --------------------------------------------------------------------- DECODING

    fun readFrom(inputStream: InputStream): String? {
        with(IppInputStream(inputStream)) {
            version = readVersion()
            code = readCode()
            requestId = readRequestId()
            lateinit var currentGroup: IppAttributesGroup
            loop@ while (true) {
                val tag = readTag()
                when {
                    tag == IppTag.End -> break@loop
                    tag.isGroupTag() -> currentGroup = newAttributesGroup(tag)
                    else -> currentGroup.put(readAttribute(tag))
                }
            }
            close()
            return statusMessage;
        }
    }

    // --------------------------------------------------------------------- LOGGING

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
        for (group in attributesGroups)
            group.logDetails(prefix)
    }
}