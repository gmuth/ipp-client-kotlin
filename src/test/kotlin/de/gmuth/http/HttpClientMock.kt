package de.gmuth.http

import de.gmuth.ipp.core.IppResponse
import de.gmuth.log.Logging
import java.io.*
import java.net.URI

class HttpClientMock(config: Http.Config = Http.Config()) : Http.Client(config) {

    companion object {
        val log = Logging.getLogger {}
    }

    lateinit var rawIppRequest: ByteArray
    var httpStatus: Int = 200
    var httpServer: String? = "HttpClientMock"
    var httpContentType: String? = "application/ipp"
    var httpContentStream: InputStream? = null
    var ippResponse: IppResponse? = null

    fun mockResponse(file: File) {
        httpContentStream = FileInputStream(file)
    }

    fun mockResponse(fileName: String, directory: String = "printers") =
            mockResponse(File(directory, fileName))

    override fun post(uri: URI, contentType: String, writeContent: (OutputStream) -> Unit, chunked: Boolean) =
            Http.Response(
                    httpStatus, httpServer, httpContentType, ippResponse?.encodeAsInputStream() ?: httpContentStream
            ).apply {
                rawIppRequest = ByteArrayOutputStream().also { writeContent(it) }.toByteArray()
                log.info { "post ${rawIppRequest.size} bytes ipp request to $uri -> response '$server', $status, ${this.contentType}" }
            }
}