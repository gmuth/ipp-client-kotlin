package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute

interface IppJobParameter {

    fun toIppAttribute(): IppAttribute<*>

    fun registeredValue(name: String) = name
            .replace("[A-Z]".toRegex()) { "-" + it.value.toLowerCase() }
            .replace("^-".toRegex(), "")

}