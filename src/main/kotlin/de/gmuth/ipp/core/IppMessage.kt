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
    var code: Short? = null
    abstract val codeDescription: String // request operation or response status
    var requestId: Int? = null
    var attributesCharset: Charset? = null
    val attributesGroups = mutableListOf<IppAttributesGroup>()

    fun newAttributesGroup(tag: IppTag): IppAttributesGroup {
        val group = IppAttributesGroup(tag)
        attributesGroups.add(group)
        return group
    }

    fun getAttributesGroups(tag: IppTag) = attributesGroups.filter { it.tag == tag }

    fun getSingleAttributesGroup(tag: IppTag) = getAttributesGroups(tag).single()

    // --------------------------------------------------------------------- ENCODING

    private fun writeTo(outputStream: OutputStream) {
        if (version == null) throw IllegalArgumentException("version must not be null")
        if (code == null) throw IllegalArgumentException("code must not be null")
        if (requestId == null) throw IllegalArgumentException("requestId must not be null")
        if (attributesCharset == null) throw IllegalArgumentException("attributesCharset must not be null")

        val ippOutputStream = IppOutputStream(outputStream, attributesCharset as Charset)
        ippOutputStream.writeMessage(this)
        ippOutputStream.close()
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

    fun readFrom(inputStream: InputStream) {//: String? {
        val ippInputStream = IppInputStream(inputStream)
        ippInputStream.readMessage(this)
        ippInputStream.close()
        //return ippInputStream.statusMessage
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