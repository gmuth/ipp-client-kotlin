package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword

enum class Sides(private val keyword: String) : IppAttributeBuilder {

    OneSided("one-sided"),
    TwoSidedLongEdge("two-sided-long-edge"),
    TwoSidedShortEdge("two-sided-short-edge");

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
            IppAttribute("sides", Keyword, keyword)

}