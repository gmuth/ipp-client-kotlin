package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection
import de.gmuth.ipp.core.IppTag.BegCollection
import de.gmuth.ipp.core.IppTag.Integer

// unit: 1/100 mm, e.g. 2540 = 1 inch
class MediaSize(val xDimension: Int, val yDimension: Int) : IppAttributeBuilder {
    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute(
            "media-size", BegCollection, IppCollection(
                IppAttribute("x-dimension", Integer, xDimension),
                IppAttribute("y-dimension", Integer, yDimension)
            )
        )
}