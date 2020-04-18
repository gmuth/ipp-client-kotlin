package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.*

abstract class IppMessage {

    var version: IppVersion? = null
    var code: Short? = null
    abstract val codeDescription: String // request operation or response status
    var requestId: Int? = null
    val attributesGroups = mutableListOf<IppAttributesGroup>()

    fun getAttributesGroups(tag: IppTag) = attributesGroups.filter { it.tag == tag }

    fun getSingleAttributesGroup(tag: IppTag) = getAttributesGroups(tag).single()

    fun ippAttributesGroup(tag: IppTag): IppAttributesGroup {
        val group = IppAttributesGroup(tag)
        attributesGroups.add(group)
        return group
    }

    val operationGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Operation)

    // --- DECODING

    fun readFrom(inputStream: InputStream) {
        val ippInputStream = IppInputStream(inputStream)
        ippInputStream.readMessage(this)
        ippInputStream.close()
    }

    fun readFromResource(resource: String) {
        readFrom(javaClass.getResourceAsStream(resource))
    }

    fun readFrom(byteArray: ByteArray) {
        readFrom(ByteArrayInputStream(byteArray))
    }

    fun readFrom(file: File) {
        readFrom(FileInputStream(file))
    }

    // --- ENCODING

    private fun writeTo(outputStream: OutputStream) {
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

    fun writeToFile(file: File) {
        writeTo(FileOutputStream(file))
    }

    // --- LOGGING

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