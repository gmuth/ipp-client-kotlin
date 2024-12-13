package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppResolution
import de.gmuth.ipp.core.IppResolution.Unit
import de.gmuth.ipp.core.IppResolution.Unit.DPI
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.*

/**
 * Create usual job attributes
 */
object TemplateAttributes {

    // For operation group

    @JvmStatic
    fun jobName(name: String) =
        IppAttribute("job-name", NameWithoutLanguage, name)

    // For job group

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
    fun finishings(values: Collection<Finishing>) =
        IppAttribute("finishings", IppTag.Enum, values.map { it.code })

    @JvmStatic
    fun orientationRequested(orientation: Orientation) =
        IppAttribute("orientation-requested", IppTag.Enum, orientation.code)

    @JvmStatic
    fun outputBin(keyword: String) = // PWG 5100.2
        IppAttribute("output-bin", Keyword, keyword)

    @JvmStatic
    fun mediaSource(keyword: String) =
        IppAttribute("media-source", Keyword, keyword)

    @JvmStatic // input tray
    fun mediaColWithSource(mediaSource: MediaSource) =
        MediaCollection(source = mediaSource)

    @JvmStatic // input tray
    fun mediaColWithSource(keyword: String) =
        mediaColWithSource(MediaSource(keyword))

    @JvmStatic // unit: hundreds of mm
    fun mediaColWithSize(xDimension: Int, yDimension: Int) =
        MediaCollection(size = MediaSize(xDimension, yDimension))

    // support vararg parameter for convenience

    @JvmStatic
    fun pageRanges(vararg ranges: IntRange) = pageRanges(ranges.toList())

    @JvmStatic
    fun finishings(vararg finishings: Finishing) = finishings(finishings.toList())

}