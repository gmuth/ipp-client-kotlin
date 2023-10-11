package de.gmuth.ipp.client

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus
import de.gmuth.log.Logging
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI

class IppClientMock(var directory: String = "printers") : IppClient() {

    init {
        Logging.configure()
        mockResponse(IppResponse(IppStatus.SuccessfulOk))
    }

    lateinit var rawResponse: ByteArray

    fun mockResponse(response: IppResponse) {
        rawResponse = response.encode()
    }

    fun mockResponse(file: File) {
        rawResponse = file.readBytes()
    }

    fun mockResponse(fileName: String, directory: String = this.directory) {
        mockResponse(File(directory, fileName))
    }

    // when used with real http, responses are frequently created and garbage collected
    // however references to attribute groups are kept in IPP objects
    // changes to an attribute group would affect other tests as well
    // therefor it's important to produce a fresh response for each call

    override fun httpPost(httpUri: URI, request: IppRequest) = IppResponse().apply {
        ByteArrayOutputStream()
            .also { request.write(it) }
            .toByteArray()
            .run { log.info { "post $size request bytes to $httpUri, ${rawResponse.size} response bytes" } }
        decode(rawResponse)
    }
}