package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppResolution
import de.gmuth.ipp.core.IppResolution.Unit
import de.gmuth.ipp.core.IppResolution.Unit.DPI
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.toIppString

/**
 * create common job attributes
 */
object IppTemplateAttributes {

    // for operation group

    @JvmStatic
    fun documentFormat(value: String) =
            IppAttribute("document-format", IppTag.MimeMediaType, value)

    @JvmStatic
    fun documentName(value: String) =
            IppAttribute("document-name", IppTag.NameWithoutLanguage, value.toIppString())

    @JvmStatic
    fun jobName(value: String) =
            IppAttribute("job-name", IppTag.NameWithoutLanguage, value.toIppString())

    // for job group

    @JvmStatic
    fun jobPriority(value: Int) =
            IppAttribute("job-priority", IppTag.Integer, value)

    @JvmStatic
    fun copies(value: Int) =
            IppAttribute("copies", IppTag.Integer, value)

    @JvmStatic
    fun numberUp(value: Int) =
            IppAttribute("number-up", IppTag.Integer, value)

    @JvmStatic
    fun printerResolution(value: Int, unit: Unit = DPI) =
            IppAttribute("printer-resolution", IppTag.Resolution, IppResolution(value, unit))

    @JvmStatic
    fun pageRanges(vararg ranges: IntRange) =
            IppAttribute("page-ranges", IppTag.RangeOfInteger, ranges.toList())

    @JvmStatic
    fun media(keyword: String) =
            IppAttribute("media", IppTag.Keyword, keyword)

}