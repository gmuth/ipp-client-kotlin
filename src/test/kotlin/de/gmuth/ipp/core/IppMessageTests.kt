package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.log.Logging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Files.createTempFile
import java.util.logging.Logger.getLogger
import kotlin.test.*

internal fun IppMessage.readTestResource(resource: String) =
    read(javaClass.getResourceAsStream(resource))

class IppMessageTests {

    private val logger = getLogger(javaClass.name)

    @BeforeTest
    fun setUp() { Logging.configure() }

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
                write(tmpFile, true)
            } finally {
                Files.delete(tmpFile)
            }
            assertEquals(38, rawBytes!!.size)
            assertFailsWith<IppException> {
                write(ByteArrayOutputStream())
            }.apply {
                logger.info(toString())
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
                assertTrue(hasDocument())
                write(Files.newOutputStream(tmpFile0), true)
                saveDocument(tmpFile1)
                assertEquals(26, Files.size(tmpFile1))
                val ippBytes = encode(appendDocumentIfAvailable = false) // trigger saving raw bytes
                assertEquals(38, ippBytes.size)
                saveBytes(tmpFile2)
                assertEquals(38, Files.size(tmpFile2))
            } finally {
                Files.delete(tmpFile1)
                Files.delete(tmpFile2)
            }
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