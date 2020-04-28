package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppIntegerRange
import de.gmuth.ipp.core.IppTag

class IppPageRanges(vararg ranges: IntRange) :
        IppAttribute<IppIntegerRange>(
                "page-ranges",
                IppTag.RangeOfInteger,
                ranges.map {
                    IppIntegerRange(it.first, it.last)
                }.toList()
        )