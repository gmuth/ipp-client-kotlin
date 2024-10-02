package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword

enum class Compression(private val keyword: String) : IppAttributeBuilder {

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
}