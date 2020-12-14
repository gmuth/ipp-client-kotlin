package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
            if (documentInputStreamIsConsumed) {
                assertFailsWith<IllegalStateException> { write(ByteArrayOutputStream()) }
            }
        }
    }

}