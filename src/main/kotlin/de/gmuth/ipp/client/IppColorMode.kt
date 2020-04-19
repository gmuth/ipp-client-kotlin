package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppTag

enum class IppColorMode : IppJobParameter {
    Auto, Color, Monochrome;

    override fun toIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<String> {
        // use job-creation-attributes-supported? // 5100.11
        val attributeName = when {
            printerAttributes["print-color-mode-supported"] != null -> "print-color-mode" // 5100.14 ipp everywhere
            printerAttributes["output-mode-supported"] != null -> "output-mode" // cups extension
            else -> throw IppException("printer does not support 'output-mode' or 'print-color-mode'")
        }
        return IppAttribute(attributeName, IppTag.Keyword, registeredValue(name))
    }
}

class IppMonochrome() : IppJobParameter {
    override fun toIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        return IppColorMode.Monochrome.toIppAttribute(printerAttributes)
    }
}