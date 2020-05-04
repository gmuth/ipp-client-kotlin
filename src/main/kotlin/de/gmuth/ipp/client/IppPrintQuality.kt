package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeHolder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag

enum class IppPrintQuality(val value: Int) : IppAttributeHolder {
    Draft(3),
    Normal(4),
    High(5);

    override fun getIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        printerAttributes.checkValueSupported("print-quality-supported", value)
        return IppAttribute("print-quality", IppTag.Enum, value)
    }
}