package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Enum

enum class OrientationRequested(private val code: Int) : IppAttributeBuilder {

    Portrait(3),
    Landscape(4),
    ReverseLandscape(5),
    ReversePortrait(6),
    None(7); // PWG 5100.13

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
            IppAttribute("orientation-requested", Enum, code)

}