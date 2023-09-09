package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword
import de.gmuth.log.warn
import java.util.logging.Logger.getLogger

enum class IppColorMode(private val keyword: String) : IppAttributeBuilder {

    Auto("auto"),
    Color("color"),
    Monochrome("monochrome");

    val log = getLogger(javaClass.name)

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<String> {
        // use job-creation-attributes-supported? // 5100.11
        val modeAttributeName = when {
            printerAttributes.containsKey("print-color-mode-supported") -> "print-color-mode" // 5100.14 ipp everywhere
            printerAttributes.containsKey("output-mode-supported") -> "output-mode" // cups extension
            else -> {
                if (printerAttributes.isEmpty()) log.warn { "no printer attributes" }
                else log.warn { "printer does not support 'output-mode' or 'print-color-mode'" }
                "print-color-mode"
            }
        }
        return IppAttribute(modeAttributeName, Keyword, keyword)
    }

}