package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

interface IppAttributeHolder<T> {

    fun getIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<T>

}