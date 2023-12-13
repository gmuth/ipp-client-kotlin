package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppTag.*

// Unit: 1/100 mm, e.g. 2540 = 1 inch
data class MediaSize(val xDimension: Int, val yDimension: Int, val name: String? = null) : IppAttributeBuilder {

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) = IppAttribute(
        "media-size", BegCollection,
        IppCollection(
            IppAttribute("x-dimension", Integer, xDimension),
            IppAttribute("y-dimension", Integer, yDimension)
        ).apply {
            name?.run { addAttribute("media-size-name", NameWithoutLanguage, this) }
        }
    )

    override fun toString() = StringBuilder().run {
        name?.let { append("$it ") }
        append("${xDimension} x ${yDimension}")
        toString()
    }

    fun equalsByDimensions(other: MediaSize) =
        compareByDimensions.compare(this, other) == 0

    companion object {
        fun fromIppCollection(ippCollection: IppCollection) = ippCollection.run {
            MediaSize(
                getValue("x-dimension"),
                getValue("y-dimension"),
                getValueOrNull<IppString>("media-size-name")?.text
            )
        }

        val compareByDimensions = compareBy(MediaSize::xDimension, MediaSize::yDimension)

        @JvmField
        val ISO_A0 = MediaSize(84100, 118900)

        @JvmField
        val ISO_A1 = MediaSize(59400, 84100)

        @JvmField
        val ISO_A2 = MediaSize(42000, 59400)

        @JvmField
        val ISO_A3 = MediaSize(29700, 42000)

        @JvmField
        val ISO_A4 = MediaSize(21000, 29700)

        @JvmField
        val ISO_A5 = MediaSize(14800, 21000)

        @JvmField
        val ISO_A6 = MediaSize(10500, 14800)

        @JvmField
        val ISO_A7 = MediaSize(7400, 10500)

        @JvmField
        val ISO_A8 = MediaSize(5200, 7400)

        @JvmField
        val ISO_A9 = MediaSize(3700, 5200)
    }
}