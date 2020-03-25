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

    // e.g. readFrom InputStream will set these members
    var version: IppVersion? = null
    var requestId: Int? = null
    var attributesCharset: Charset? = null
    var naturalLanguage: String? = null

    protected var code: Short? = null
    abstract val codeDescription: String

    var operationGroup: IppAttributesGroup? = null
    var jobGroups = mutableListOf<IppAttributesGroup>()

    fun addOperationAttribute(name: String, tag: IppTag, value: Any) = operationGroup?.put(name, tag, value)
    fun addOperationAttribute(name: String, value: Any) = operationGroup?.put(name, value)

    // --------------------------------------------------------------------- IPP MESSAGE ENCODING

    private fun writeTo(outputStream: OutputStream) {
        if (version == null) throw IllegalArgumentException("version must not be null")
        if (code == null) throw IllegalArgumentException("code must not be null")
        if (requestId == null) throw IllegalArgumentException("requestId must not be null")
        if (attributesCharset == null) throw IllegalArgumentException("attributesCharset must not be null")
        if (naturalLanguage == null) throw IllegalArgumentException("naturalLanguage must not be null")
        if (operationGroup == null) throw java.lang.IllegalArgumentException("operationGroup must not be null")

        with(IppOutputStream(outputStream, attributesCharset as Charset)) {
            writeVersion(version as IppVersion)
            writeCode(code as Short)
            writeRequestId(requestId as Int)
            writeGroup(operationGroup as IppAttributesGroup)
            for (jobGroup in jobGroups) {
                writeGroup(jobGroup)
            }
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

    // --------------------------------------------------------------------- IPP MESSAGE DECODING

    fun readFrom(inputStream: InputStream): String? {
        val ippInputStream = IppInputStream(inputStream)

        version = ippInputStream.readVersion()
        code = ippInputStream.readCode()
        requestId = ippInputStream.readRequestId()

        var tag: IppTag
        var currentGroup: IppAttributesGroup? = null
        do {
            tag = ippInputStream.readTag()
            if (tag.isGroupTag()) {
                if (tag != IppTag.End) {
                    currentGroup = IppAttributesGroup(tag)
                    when (tag) {
                        IppTag.Operation -> operationGroup = currentGroup
                        IppTag.Job -> jobGroups.add(currentGroup)
                    }
                }
            } else {
                val attribute = ippInputStream.readAttribute(tag)
                if (currentGroup == null)
                    throw IppSpecViolation("unable to put attribute to a group because a delimiter wasn't found yet")
                else
                    currentGroup.put(attribute)
            }
        } while (tag != IppTag.End)
        ippInputStream.close()
        return ippInputStream.statusMessage;
    }

    // --------------------------------------------------------------------- LOGGING

    fun logDetails(prefix : String) {
        println("$prefix version = $version")
        println("$prefix $codeDescription")
        println("$prefix request-id = $requestId")
        operationGroup?.logDetails(prefix)
        for (job in jobGroups) job.logDetails(prefix)
    }

}