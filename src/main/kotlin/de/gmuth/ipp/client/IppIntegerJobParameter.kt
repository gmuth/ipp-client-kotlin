package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppTag

class IppIntegerJobParameter(
        val name: String,
        vararg val values: Int

) : IppJobParameter {

    override fun toIppAttribute(printer: IppPrinter?): IppAttribute<Int> =
            IppAttribute(name, IppTag.Integer, values.toList())

}