package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag

enum class IppSides : IppJobParameter {
    OneSided, TwoSidedLongEdge, TwoSidedShortEdge;

    override fun toIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        return IppAttribute("sides", IppTag.Keyword, registeredValue(name))
    }
}

class IppDuplex : IppJobParameter {
    override fun toIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        return IppSides.TwoSidedLongEdge.toIppAttribute(printerAttributes)
    }
}