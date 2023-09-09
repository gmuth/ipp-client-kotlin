package de.gmuth.http

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.io.ByteArray
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus
import java.io.*
import java.net.URI
import java.util.logging.Logger.getLogger

class HttpClientMock(config: Http.Config = Http.Config()) : Http.Client(config) {

    val log = getLogger(javaClass.name)
    lateinit var rawIppRequest: ByteArray
    var httpStatus: Int = 200
    var httpServer: String? = "HttpClientMock"
    var httpContentType: String? = "application/ipp"
    var ippResponse: IppResponse? = IppResponse(IppStatus.SuccessfulOk)
    var httpContentFile: File? = null

    fun mockResponse(file: File) {
        httpContentFile = file
        ippResponse = null
    }

    fun mockResponse(fileName: String, directory: String = "printers") =
            mockResponse(File(directory, fileName))

    override fun post(uri: URI, contentType: String, writeContent: (OutputStream) -> Unit, chunked: Boolean) =
            Http.Response(
                    httpStatus, httpServer, httpContentType, ippResponse?.encodeAsInputStream() ?: httpContentFile?.inputStream()
            ).apply {
                rawIppRequest = ByteArray(writeContent)
                log.info { "post ${rawIppRequest.size} bytes ipp request to $uri -> response '$server', $status, ${this.contentType}" }
            }
}