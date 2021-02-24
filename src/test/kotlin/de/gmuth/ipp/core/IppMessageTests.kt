package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.ByteArrayOutputStream
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
            version = IppVersion()
            requestId = 5
            code = 0
            val tmpFile = createTempFile()
            try {
                write(tmpFile)
            } finally {
                tmpFile.delete()
            }
            assertTrue(documentInputStreamIsConsumed)
            assertEquals(38, rawBytes!!.size)
            write(ByteArrayOutputStream()) // cover warning
        }
    }

    @Test
    fun saveDocument() {
        with(message) {
            getSingleAttributesGroup(IppTag.Operation, true).attribute("attributes-charset", IppTag.Charset, Charsets.UTF_8)
            version = IppVersion()
            requestId = 7
            code = 0
            documentInputStream = "Lorem ipsum dolor sit amet".byteInputStream()
            val tmpFile = createTempFile()
            try {
                saveDocument(tmpFile)
                assertEquals(26, tmpFile.length())
            } finally {
                tmpFile.delete()
            }
            assertTrue(documentInputStreamIsConsumed)
        }
    }

}