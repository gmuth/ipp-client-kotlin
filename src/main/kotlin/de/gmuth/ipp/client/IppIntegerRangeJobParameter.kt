package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppIntegerRange
import de.gmuth.ipp.core.IppTag

class IppIntegerRangeJobParameter(
        val name: String,
        val ranges: List<IntRange>

) : IppJobParameter {

    constructor(name: String, vararg ranges: IntRange) : this(name, ranges.toList())

    override fun toIppAttribute(printer: IppPrinter?): IppAttribute<IppIntegerRange> = IppAttribute(
            name,
            IppTag.RangeOfInteger,
            ranges.map { IppIntegerRange(it.first, it.last) }
    )

}