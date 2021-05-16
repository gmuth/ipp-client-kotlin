package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.ByteArrayOutputStream
import java.io.File.createTempFile
import kotlin.test.*

class IppMessageTests {

    private val message = object : IppMessage() {
        override val codeDescription: String
            get() = "codeDescription"
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
    fun writeFile() {
        with(message) {
            getSingleAttributesGroup(IppTag.Operation, true).attribute("attributes-charset", IppTag.Charset, Charsets.UTF_8)
            version = "1.1"
            requestId = 5
            code = 0
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
            logDetails() // cover logDetails
        }
    }

    @Test
    fun saveDocumentAndIpp() {
        with(message) {
            getSingleAttributesGroup(IppTag.Operation, true).attribute("attributes-charset", IppTag.Charset, Charsets.UTF_8)
            version =  "1.1"
            requestId = 7
            code = 0
            documentInputStream = "Lorem ipsum dolor sit amet".byteInputStream()
            val tmpFile1 = createTempFile("test", null)
            val tmpFile2 = createTempFile("test", null)
            try {
                saveDocumentStream(tmpFile1)
                assertEquals(26, tmpFile1.length())
                IppMessage.saveRawBytes = true
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
        assertEquals("codeDescription []",message.toString())
        message.logDetails()
        assertFailsWith<RuntimeException> {
            // missing raw bytes
            with(createTempFile("test",null)) {
                try {
                    message.saveRawBytes(this)
                } finally {
                    this.delete()
                }
            }
        }
    }

}