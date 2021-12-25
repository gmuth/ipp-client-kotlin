package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-21 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppResolution
import de.gmuth.ipp.core.IppResolution.Unit
import de.gmuth.ipp.core.IppResolution.Unit.DPI
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.ipp.core.toIppString

/**
 * create common job attributes
 */
object IppTemplateAttributes {

    // for operation group

    @JvmStatic
    fun documentFormat(format: String) =
            IppAttribute("document-format", MimeMediaType, format)

    @JvmStatic
    fun jobName(name: String) =
            IppAttribute("job-name", NameWithoutLanguage, name.toIppString())

    // for job group

    @JvmStatic
    fun jobPriority(priority: Int) =
            IppAttribute("job-priority", Integer, priority)

    @JvmStatic
    fun copies(number: Int) =
            IppAttribute("copies", Integer, number)

    @JvmStatic
    fun numberUp(up: Int) =
            IppAttribute("number-up", Integer, up)

    @JvmStatic
    fun printerResolution(resolution: Int, unit: Unit = DPI) =
            IppAttribute("printer-resolution", Resolution, IppResolution(resolution, unit))

    @JvmStatic
    fun pageRanges(ranges: Collection<IntRange>) =
        IppAttribute("page-ranges", RangeOfInteger, ranges)

    @JvmStatic
    fun media(keyword: String) =
            IppAttribute("media", Keyword, keyword)

    @JvmStatic
    fun finishings(finishings: Collection<IppFinishing>) =
            IppAttribute("finishings", IppTag.Enum, finishings.map { it.code })

    // support vararg parameter for convenience

    @JvmStatic
    fun pageRanges(vararg ranges: IntRange) = pageRanges(ranges.toList())

    @JvmStatic
    fun finishings(vararg finishings: IppFinishing) = finishings(finishings.toList())

}