package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag

enum class IppSides(val value: String) : IppJobParameter {
    OneSided("one-sided"),
    TwoSidedLongEdge("two-sided-longe-edge"),
    TwoSidedShortEdge("two-sided-short-edge");

    override fun toIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        printerAttributes.assertValueSupported("sides-supported", value)
        return IppAttribute("sides", IppTag.Keyword, value)
    }
}

class IppDuplex : IppJobParameter {
    override fun toIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        return IppSides.TwoSidedLongEdge.toIppAttribute(printerAttributes)
    }
}