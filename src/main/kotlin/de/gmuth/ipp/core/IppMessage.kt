package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

abstract class IppMessage {

    var version: IppVersion? = null
    var code: Short? = null
    abstract val codeDescription: String // request operation or response status
    var requestId: Int? = null
    val attributesGroups = mutableListOf<IppAttributesGroup>()

    fun getAttributesGroups(tag: IppTag) = attributesGroups.filter { it.tag == tag }

    fun getSingleAttributesGroup(tag: IppTag) = getAttributesGroups(tag).single()

    fun newAttributesGroup(tag: IppTag): IppAttributesGroup {
        val group = IppAttributesGroup(tag)
        attributesGroups.add(group)
        return group
    }

    // --------------------------------------------------------------------- DECODING

    fun readFrom(inputStream: InputStream) {
        val ippInputStream = IppInputStream(inputStream)
        ippInputStream.readMessage(this)
        ippInputStream.close()
    }

    // --------------------------------------------------------------------- ENCODING

    private fun writeTo(outputStream: OutputStream) {
        //if (code == null) throw IppException("code must not be null")
        //if (requestId == null) throw IppException("requestId must not be null")

        val ippOutputStream = IppOutputStream(outputStream)
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
        attributesGroups.forEach { group -> group.logDetails(prefix) }
    }
}