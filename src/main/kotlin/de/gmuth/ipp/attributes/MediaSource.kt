package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword
import java.util.logging.Logger.getLogger

open class MediaSource(val keyword: String) : IppAttributeBuilder {

    val log = getLogger(javaClass.name)

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("media-source", Keyword, keyword)
            .apply { validateSource(printerAttributes) }

    private fun validateSource(printerAttributes: IppAttributesGroup) {
        val mediaSourceSupported = printerAttributes["media-source-supported"]
        if (mediaSourceSupported == null) {
            log.fine { "printer does not provide attribute 'media-source-supported'" }
        } else {
            if (!mediaSourceSupported.values.contains(keyword)) {
                log.warning { "media-source '$keyword' not supported by printer" }
                log.warning { mediaSourceSupported.toString() }
            }
        }
    }
}