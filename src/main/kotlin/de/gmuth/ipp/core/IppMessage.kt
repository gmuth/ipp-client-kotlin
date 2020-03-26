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

    val operationGroup = IppAttributesGroup(IppTag.Operation)
    val jobGroups = mutableListOf<IppAttributesGroup>()

    fun addOperationAttribute(name: String, tag: IppTag, value: Any?) {
        if (value != null) operationGroup.put(name, tag, value)
    }

    fun addOperationAttribute(name: String, value: Any?) {
        if (value != null) operationGroup.put(name, value)
    }

    private fun addNewJobGroup(): IppAttributesGroup {
        val jobGroup = IppAttributesGroup(IppTag.Job)
        jobGroups.add(jobGroup)
        return jobGroup
    }

    fun getSingleJobGroup(): IppAttributesGroup =
            if (jobGroups.size == 1) jobGroups.first()
            else throw IllegalStateException("found ${jobGroups.size.toPluralString("job group")}")

    // --------------------------------------------------------------------- ENCODING

    private fun writeTo(outputStream: OutputStream) {
        if (version == null) throw IllegalArgumentException("version must not be null")
        if (code == null) throw IllegalArgumentException("code must not be null")
        if (requestId == null) throw IllegalArgumentException("requestId must not be null")
        if (attributesCharset == null) throw IllegalArgumentException("attributesCharset must not be null")
        if (naturalLanguage == null) throw IllegalArgumentException("naturalLanguage must not be null")

        with(IppOutputStream(outputStream, attributesCharset as Charset)) {
            writeVersion(version as IppVersion)
            writeCode(code as Short)
            writeRequestId(requestId as Int)
            writeAttributesGroup(operationGroup)
            jobGroups.forEach { jobGroup -> writeAttributesGroup(jobGroup) }
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
                    when (tag) {
                        IppTag.Operation -> currentGroup = operationGroup
                        IppTag.Job -> currentGroup = addNewJobGroup()
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

    override fun toString(): String = String.format(
            "%s: %s, %s, %s",
            javaClass.simpleName,
            codeDescription,
            operationGroup.size.toPluralString("operation attribute"),
            jobGroups.size.toPluralString("job group")
    )

    fun logDetails(prefix: String) {
        println("$prefix version = $version")
        println("$prefix $codeDescription")
        println("$prefix request-id = $requestId")
        operationGroup.logDetails(prefix)
        for (job in jobGroups) job.logDetails(prefix)
    }

}