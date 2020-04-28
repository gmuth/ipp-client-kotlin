package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeHolder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag

enum class IppSides(val value: String) : IppAttributeHolder {
    OneSided("one-sided"),
    TwoSidedLongEdge("two-sided-long-edge"),
    TwoSidedShortEdge("two-sided-short-edge");

    override fun getIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        printerAttributes.checkValueSupported("sides-supported", value)
        return IppAttribute("sides", IppTag.Keyword, value)
    }
}

class IppDuplex : IppAttributeHolder {
    override fun getIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        return IppSides.TwoSidedLongEdge.getIppAttribute(printerAttributes)
    }
}