package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File.createTempFile
import java.util.logging.Logger.getLogger
import kotlin.test.*

internal fun IppMessage.readTestResource(resource: String) =
    read(javaClass.getResourceAsStream(resource))

class IppMessageTests {

    private val logger = getLogger(javaClass.name)

    private val message = object : IppMessage() {
        override val codeDescription: String
            get() = "codeDescription"
    }

    @Test
    fun setVersionFails() {
        assertFailsWith<IppException> { message.version = "wrong" }
    }

    @Test
    fun getSingleAttributesGroupFails() {
        assertFailsWith<IppException> { message.getSingleAttributesGroup(IppTag.Operation) }
    }

    @Test
    fun containsGroup() {
        assertFalse(message.containsGroup(IppTag.Job))
    }

    @Test
    fun hasNoDocument() {
        assertFalse(message.hasDocument())
    }

    @Test
    fun writeFile() {
        with(message) {
            createAttributesGroup(IppTag.Operation).attribute("attributes-charset", IppTag.Charset, Charsets.UTF_8)
            version = "1.1"
            requestId = 5
            code = 0
            documentInputStream = ByteArrayInputStream("01 02 03".toByteArray())
            val tmpFile = createTempFile("test", null)
            try {
                write(tmpFile)
            } finally {
                tmpFile.delete()
            }
            assertTrue(documentInputStreamIsConsumed)
            assertEquals(38, rawBytes!!.size)
            assertFailsWith<IppException> {
                write(ByteArrayOutputStream())
            }
            toString() // cover toString
            log(logger) // cover log
        }
    }

    @Test
    fun saveDocumentAndIpp() {
        with(message) {
            createAttributesGroup(IppTag.Operation).attribute("attributes-charset", IppTag.Charset, Charsets.UTF_8)
            version = "1.1"
            requestId = 7
            code = 0
            documentInputStream = "Lorem ipsum dolor sit amet".byteInputStream()
            val tmpFile0 = createTempFile("test", null)
            val tmpFile1 = createTempFile("test", null)
            val tmpFile2 = createTempFile("test", null)
            try {
                IppMessage.keepDocumentCopy = true
                write(tmpFile0.outputStream())
                assertTrue(hasDocument())
                saveDocumentBytes(tmpFile1)
                assertEquals(26, tmpFile1.length())
                val ippBytes = encode(appendDocumentIfAvailable = false) // trigger saving raw bytes
                assertEquals(38, ippBytes.size)
                saveBytes(tmpFile2)
                assertEquals(38, tmpFile2.length())
            } finally {
                tmpFile1.delete()
                tmpFile2.delete()
            }
            assertTrue(documentInputStreamIsConsumed)
        }
    }

    @Test
    fun withoutRawBytes() {
        message.log(logger)
        assertFailsWith<IppException> { // missing raw bytes
            message.saveBytes(createTempFile("rawbytes", null))
        }
    }

    @Test
    fun writeTest() {
        message.saveText(createTempFile("text", null))
    }

}