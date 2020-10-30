package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppResolution
import de.gmuth.ipp.core.IppResolution.Unit
import de.gmuth.ipp.core.IppResolution.Unit.DPI
import de.gmuth.ipp.core.IppTag

/**
 * methods to create common job attributes
 */
object IppJobTemplateAttribute {

    fun attribute(name: String, tag: IppTag, vararg values: Any) =
            IppAttribute(name, tag, values.toList())

    fun documentFormat(value: String) =
            IppAttribute("document-format", IppTag.MimeMediaType, value)

    fun documentName(value: String) =
            IppAttribute("document-Name", IppTag.NameWithoutLanguage, value)

    fun jobName(value: String) =
            IppAttribute("job-name", IppTag.NameWithoutLanguage, value)

    fun jobPriority(value: Int) =
            IppAttribute("job-priority", IppTag.Integer, value)

    fun copies(value: Int) =
            IppAttribute("copies", IppTag.Integer, value)

    fun numberUp(value: Int) =
            IppAttribute("number-up", IppTag.Integer, value)

    fun printerResolution(value: Int, unit: Unit = DPI) =
            IppAttribute("printer-resolution", IppTag.Resolution, IppResolution(value, unit))

    fun pageRanges(vararg ranges: IntRange) =
            IppAttribute("page-ranges", IppTag.RangeOfInteger, ranges.toList())

    fun media(value: String) =
            IppAttribute("media", IppTag.Keyword, value)

    fun mediaCol(xDimension: Int, yDimension: Int, margin: Int? = null) =
            IppMediaCollection(IppMediaCollection.Size(xDimension, yDimension)).apply {
                if (margin != null) this.margins = IppMediaCollection.Margins(margin)
            }

}