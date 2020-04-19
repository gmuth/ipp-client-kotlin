package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributesGroup

interface IppJobParameter {

    fun toIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*>

    fun registeredValue(name: String) = name
            .replace("[A-Z]".toRegex()) { "-" + it.value.toLowerCase() }
            .replace("^-".toRegex(), "")

}