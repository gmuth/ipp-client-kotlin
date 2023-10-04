package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.MimeMediaType

open class DocumentFormat(val mediaMimeType: String) : IppAttributeBuilder {

    // application
    object OCTET_STREAM : DocumentFormat("application/octet-stream")
    object POSTSCRIPT : DocumentFormat("application/postscript")
    object PDF : DocumentFormat("application/pdf")

    // image
    object PWG_RASTER : DocumentFormat("image/pwg-raster")
    object TIFF : DocumentFormat("image/tiff")
    object JPEG : DocumentFormat("image/jpeg")
    object PNG : DocumentFormat("image/png")

    // vnd
    object HP_PCL : DocumentFormat("vnd.hp-PCL")

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("document-format", MimeMediaType, mediaMimeType)

}