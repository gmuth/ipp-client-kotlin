package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.*

enum class IppColorMode(val value: String) : IppAttributeHolder<String> {

    Auto("auto"),
    Color("color"),
    Monochrome("monochrome");

    override fun getIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<String> {
        // use job-creation-attributes-supported? // 5100.11
        val modeAttributeName = when {
            printerAttributes.containsKey("print-color-mode-supported") -> "print-color-mode" // 5100.14 ipp everywhere
            printerAttributes.containsKey("output-mode-supported") -> "output-mode" // cups extension
            else -> throw IppException("printer does not support 'output-mode' or 'print-color-mode'")
        }
        return IppAttribute(modeAttributeName, IppTag.Keyword, value)
    }

}