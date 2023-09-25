package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File.createTempFile
import java.util.logging.Logger.getLogger
import kotlin.test.*

class IppMessageTests {

    val log = getLogger(javaClass.name)

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
            write(ByteArrayOutputStream()) // cover warning
            toString() // cover toString
            log(log) // cover log
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
            val tmpFile1 = createTempFile("test", null)
            val tmpFile2 = createTempFile("test", null)
            try {
                assertTrue(hasDocument())
                saveDocumentStream(tmpFile1)
                assertEquals(26, tmpFile1.length())
                encode() // save raw bytes
                saveRawBytes(tmpFile2)
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
        assertEquals("codeDescription []", message.toString())
        message.log(log)
        assertFailsWith<IppException> { // missing raw bytes
            message.saveRawBytes(createTempFile("test", null))
        }
    }

}