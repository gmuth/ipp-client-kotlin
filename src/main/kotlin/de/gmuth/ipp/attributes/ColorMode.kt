package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppTag.Keyword

enum class ColorMode(private val keyword: String) : IppAttributeBuilder {

    Auto("auto"),
    Color("color"),
    Monochrome("monochrome");

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) = IppAttribute(
        when { // use job-creation-attributes-supported? // 5100.11
            printerAttributes.containsKey("print-color-mode-supported") -> "print-color-mode" // 5100.14 IPP Everywhere
            printerAttributes.containsKey("output-mode-supported") -> "output-mode" // CUPS Extension
            else -> throw IppException(
                if (printerAttributes.isEmpty()) "Printer attributes required to choose correct attribute"
                else "Required attribute not found (print-color-mode-supported or output-mode-supported)"
            )
        },
        Keyword,
        keyword
    )
}