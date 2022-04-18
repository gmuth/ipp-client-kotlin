package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging

object IppMedia {

    val log = Logging.getLogger {}

    // unit: 1/100 mm, e.g. 2540 = 1 inch
    class Size(val xDimension: Int, val yDimension: Int) : IppAttributeBuilder {

        override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
                IppAttribute("media-size", BegCollection, IppCollection(
                        IppAttribute("x-dimension", Integer, xDimension),
                        IppAttribute("y-dimension", Integer, yDimension)
                ))
    }

    class Margins(left: Int? = null, right: Int? = null, top: Int? = null, bottom: Int? = null) : ArrayList<IppAttribute<*>>() {
        constructor(margin: Int) : this(margin, margin, margin, margin)

        init {
            if (top != null) add(IppAttribute("media-top-margin", Integer, top))
            if (left != null) add(IppAttribute("media-left-margin", Integer, left))
            if (right != null) add(IppAttribute("media-right-margin", Integer, right))
            if (bottom != null) add(IppAttribute("media-bottom-margin", Integer, bottom))
        }
    }

    // PWG 5100.3, 3.13
    class Collection(
            var size: Size? = null,
            var margins: Margins? = null,
            var source: String? = null,
            var type: String? = null

    ) : IppAttributeBuilder {

        override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
                IppAttribute("media-col", BegCollection, IppCollection().apply {
                    if (source != null) {
                        checkIfSourceIsSupported(printerAttributes)
                        addAttribute("media-source", Keyword, source!!)
                    }
                    type?.let { addAttribute("media-type", Keyword, it) }
                    size?.let { add(it.buildIppAttribute(printerAttributes)) }
                    margins?.let { addAll(it) }
                })

        private fun checkIfSourceIsSupported(printerAttributes: IppAttributesGroup) {
            val mediaSourceSupported = printerAttributes["media-source-supported"]
            if (mediaSourceSupported == null) {
                log.debug { "printer does not provide attribute 'media-source-supported'" }
            } else {
                if (!mediaSourceSupported.values.contains(source)) {
                    log.warn { "media-source '$source' not supported by printer" }
                    log.warn { mediaSourceSupported.toString() }
                }
            }
        }
    }

}