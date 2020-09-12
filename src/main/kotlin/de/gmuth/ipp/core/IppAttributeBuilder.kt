package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

interface IppAttributeBuilder {

    fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*>

}