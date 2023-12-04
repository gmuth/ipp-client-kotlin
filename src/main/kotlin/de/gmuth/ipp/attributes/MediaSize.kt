package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppTag.*

// Unit: 1/100 mm, e.g. 2540 = 1 inch
data class MediaSize(val xDimension: Int, val yDimension: Int, val name: String? = null) : IppAttributeBuilder {

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) = IppAttribute(
        "media-size", BegCollection,
        IppCollection(
            IppAttribute("x-dimension", Integer, xDimension),
            IppAttribute("y-dimension", Integer, yDimension)
        ).apply {
            name?.run { addAttribute("media-size-name", NameWithoutLanguage, this) }
        }
    )

    override fun toString() = "$name (${xDimension} x ${yDimension})"

    companion object {
        fun fromIppCollection(ippCollection: IppCollection) = ippCollection.run {
            MediaSize(
                getValue("x-dimension"),
                getValue("y-dimension"),
                getValueOrNull<IppString>("media-size-name")?.text
            )
        }
    }
}