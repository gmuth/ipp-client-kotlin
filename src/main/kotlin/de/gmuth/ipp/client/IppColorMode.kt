package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppTag

enum class IppColorMode : IppJobParameter {

    Auto, Color, Monochrome;

    // should check support for CUPS extension 'output-mode' or 'printer-color-mode'
    override fun toIppAttribute() = IppAttribute("output-mode", IppTag.Keyword, registeredValue(name))

}