package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppIntegerRange
import de.gmuth.ipp.core.IppTag

open class IppIntegerRangeJobParameter(val name: String, private val ranges: List<IntRange>) : IppJobParameter {

    override fun toIppAttribute(printer: IppPrinter?): IppAttribute<IppIntegerRange> {
        return IppAttribute(
                name,
                IppTag.RangeOfInteger,
                ranges.map { IppIntegerRange(it.first, it.last) }
        )
    }

}

class IppPageRanges(vararg ranges: IntRange) : IppIntegerRangeJobParameter("page-ranges", ranges.toList())