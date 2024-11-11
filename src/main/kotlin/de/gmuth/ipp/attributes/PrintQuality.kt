package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Enum

enum class PrintQuality(val code: Int) : IppAttributeBuilder {

    Draft(3), Normal(4), High(5);

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
            IppAttribute("print-quality", Enum, code)

}