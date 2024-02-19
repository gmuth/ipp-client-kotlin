package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.MimeMediaType

class DocumentFormat(val mediaMimeType: String) : IppAttributeBuilder {

    companion object {
        // application
        @JvmField
        val OCTET_STREAM = DocumentFormat("application/octet-stream")

        @JvmField
        val POSTSCRIPT = DocumentFormat("application/postscript")

        @JvmField
        val PDF = DocumentFormat("application/pdf")

        // image
        @JvmField
        val PWG_RASTER = DocumentFormat("image/pwg-raster")

        @JvmField
        val TIFF = DocumentFormat("image/tiff")

        @JvmField
        val JPEG = DocumentFormat("image/jpeg")

        @JvmField
        val PNG = DocumentFormat("image/png")

        // vnd
        @JvmField
        val HP_PCL = DocumentFormat("vnd.hp-PCL")
    }

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("document-format", MimeMediaType, mediaMimeType)

}