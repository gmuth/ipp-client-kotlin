package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection
import de.gmuth.ipp.core.IppTag.BegCollection
import de.gmuth.ipp.core.IppTag.Keyword
import java.util.logging.Logger.getLogger

// https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobext21-20230210-5100.7.pdf 6.3
class MediaCollection(
    var size: MediaSize? = null,
    var margin: MediaMargin? = null,
    var source: MediaSource? = null,
    var type: String? = null

) : IppAttributeBuilder {

    val log = getLogger(javaClass.name)

    fun withType(keyword: String) = this
        .apply { type = keyword }

    fun withSource(keyword: String) = this
        .apply { source = MediaSource(keyword) }

    fun withSize(xDimension: Int, yDimension: Int) = this
        .apply { size = MediaSize(xDimension, yDimension) }

    fun withMargin(value: Int) = this
        .apply { margin = MediaMargin(value, value, value, value) }

    fun withMargin(left: Int? = null, right: Int? = null, top: Int? = null, bottom: Int? = null) = this
        .apply { margin = MediaMargin(left, right, top, bottom) }

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("media-col", BegCollection, IppCollection().apply {
            type?.let { addAttribute("media-type", Keyword, it) }
            size?.let { add(it.buildIppAttribute(printerAttributes)) }
            margin?.let { addAll(it.buildIppAttributes()) }
            source?.let { add(it.buildIppAttribute(printerAttributes)) }
        })

}