package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag

enum class IppSides(private val keyword: String) : IppAttributeBuilder {

    OneSided("one-sided"),
    TwoSidedLongEdge("two-sided-long-edge"),
    TwoSidedShortEdge("two-sided-short-edge");

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
            IppAttribute("sides", IppTag.Keyword, keyword)

}