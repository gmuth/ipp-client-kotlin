package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppIntegerRange
import de.gmuth.ipp.core.IppTag

/* IppPrinter inherits from this class for convenient 'with' access to attribute 'builders'  */

open class IppJobAttributes {

    fun attribute(name: String, vararg values: Any) =
            IppAttribute(name, values.toList())

    fun documentFormat(value: String) =
            IppAttribute("document-format", IppTag.MimeMediaType, value)

    fun jobName(value: String) =
            IppAttribute("job-name", IppTag.NameWithoutLanguage, value)

    fun copies(value: Int) =
            IppAttribute("copies", IppTag.Integer, value)

    fun pageRanges(vararg ranges: IntRange) =
            IppAttribute(
                    "page-ranges",
                    IppTag.RangeOfInteger,
                    ranges.map {
                        IppIntegerRange(it.first, it.last)
                    }.toList()
            )

}