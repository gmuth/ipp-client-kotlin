package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword

class MediaSource(val keyword: String) : IppAttributeBuilder {

    companion object {
        @JvmField
        val Auto = MediaSource("auto")

        @JvmField
        val Main = MediaSource("main")

        @JvmField
        val Manual = MediaSource("manual")

        @JvmField
        val Envelope = MediaSource("envelope")

        @JvmField
        val Alternate = MediaSource("alternate")

        @JvmField
        val ByPassTray = MediaSource("by-pass-tray")

        @JvmField
        val LargeCapacity = MediaSource("large-capacity")
    }

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("media-source", Keyword, keyword)

}