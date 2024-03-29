package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

fun interface IppAttributeBuilder {

    // because some attribute names or values depend on the printers capabilities
    // we provide the printer attributes here (see also IppColorMode).
    fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*>

}