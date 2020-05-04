package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppIntegerRange
import de.gmuth.ipp.core.IppResolution
import de.gmuth.ipp.core.IppTag

class IppJobAttributes {
    companion object {

        fun attribute(name: String, vararg values: Any) =
                IppAttribute(name, values.toList())

        fun documentFormat(value: String) =
                IppAttribute("document-format", IppTag.MimeMediaType, value)

        fun jobName(value: String) =
                IppAttribute("job-name", IppTag.NameWithoutLanguage, value)

        fun copies(value: Int) =
                IppAttribute("copies", IppTag.Integer, value)

        fun printerResolutionDpi(value: Int) =
                IppAttribute("printer-resolution", IppTag.Resolution, IppResolution(value))

        fun pageRanges(vararg ranges: IntRange) =
                IppAttribute(
                        "page-ranges",
                        IppTag.RangeOfInteger,
                        ranges.map {
                            IppIntegerRange(it.first, it.last)
                        }.toList()
                )
    }
}