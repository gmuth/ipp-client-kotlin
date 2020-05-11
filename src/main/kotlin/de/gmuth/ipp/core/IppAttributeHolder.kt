package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

interface IppAttributeHolder {

    fun getIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*>

}