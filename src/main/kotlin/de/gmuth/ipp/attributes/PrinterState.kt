package de.gmuth.ipp.attributes

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

enum class PrinterState(val code: Int) : IppAttributeBuilder {

    Idle(3),
    Processing(4),
    Stopped(5);

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-6
    override fun toString() = name.lowercase()

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("printer-state", IppTag.Enum, code)

    companion object {
        private fun fromInt(code: Int) = values().single { it.code == code }
        fun fromAttributes(attributes: IppAttributesGroup) = fromInt(attributes.getValue("printer-state"))
    }

}