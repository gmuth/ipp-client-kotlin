package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword

enum class ColorMode(private val keyword: String) : IppAttributeBuilder {

    Auto("auto"),
    Color("color"),
    Monochrome("monochrome");

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) = IppAttribute(
        when { // use job-creation-attributes-supported? // 5100.11
            printerAttributes.containsKey("output-mode-supported") -> "output-mode" // cups extension
            else -> "print-color-mode" // 5100.14 ipp everywhere
        },
        Keyword,
        keyword
    )
}