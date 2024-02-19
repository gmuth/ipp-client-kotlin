package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppTag.Keyword

data class MediaSource(val keyword: String) : IppAttribute<String>("media-source", Keyword, keyword) {

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

    override fun toString() = keyword

}