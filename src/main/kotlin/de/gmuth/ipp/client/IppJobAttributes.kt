package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppIntegerRange
import de.gmuth.ipp.core.IppResolution
import de.gmuth.ipp.core.IppTag

object IppJobAttributes {

    fun attribute(name: String, tag: IppTag, vararg values: Any) =
            IppAttribute(name, tag, values.toList())

    fun documentFormat(value: String) =
            IppAttribute("document-format", IppTag.MimeMediaType, value)

    fun media(value: String) =
            IppAttribute("media", IppTag.Keyword, value)

    fun jobName(value: String) =
            IppAttribute("job-name", IppTag.NameWithoutLanguage, value)

    fun jobPriority(value: Int) =
            IppAttribute("job-priority", IppTag.Integer, value)

    fun copies(value: Int) =
            IppAttribute("copies", IppTag.Integer, value)

    fun numberUp(value: Int) =
            IppAttribute("number-up", IppTag.Integer, value)

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