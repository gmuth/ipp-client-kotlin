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

// https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobext21-20230210-5100.7.pdf 6.3
class MediaCollection(
    val size: MediaSize? = null,
    val margin: MediaMargin? = null,
    val source: MediaSource? = null,
    val type: String? = null
) : IppAttributeBuilder {

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("media-col", BegCollection, IppCollection().apply {
            type?.let { addAttribute("media-type", Keyword, it) }
            size?.let { add(it.buildIppAttribute(printerAttributes)) }
            margin?.let { addAll(it.buildIppAttributes()) }
            source?.let { add(it.buildIppAttribute(printerAttributes)) }
        })

}