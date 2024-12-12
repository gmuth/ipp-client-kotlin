package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.DeflaterInputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

enum class Compression(val keyword: String) : IppAttributeBuilder {

    COMPRESS("compress"), // RFC 1977
    DEFLATE("deflate"), // RFC 1951
    GZIP("gzip"), // RFC 1952
    NONE("none");

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("compression", Keyword, keyword)

    companion object {
        fun fromString(string: String) =
            Compression.values().single { it.keyword == string }
    }

    fun getCompressingOutputStream(outputStream: OutputStream) = when (this) {
        NONE -> outputStream
        GZIP -> GZIPOutputStream(outputStream)
        DEFLATE -> DeflaterOutputStream(outputStream)
        else -> throw NotImplementedError("compression '$this'")
    }

    fun getUncompressingInputStream(inputStream: InputStream) = when (this) {
        NONE -> inputStream
        GZIP -> GZIPInputStream(inputStream)
        DEFLATE -> DeflaterInputStream(inputStream)
        else -> throw NotImplementedError("compression '$this'")
        // Apache ZCompressorInputStream?
    }
}