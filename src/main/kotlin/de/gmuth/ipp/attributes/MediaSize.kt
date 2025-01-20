package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection
import de.gmuth.ipp.core.IppTag.BegCollection
import de.gmuth.ipp.core.IppTag.Integer

// Unit: 1/100 mm, e.g. 2540 = 1 inch
data class MediaSize(val xDimension: Int, val yDimension: Int) : IppAttributeBuilder {

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) = IppAttribute(
        "media-size", BegCollection,
        IppCollection(
            IppAttribute("x-dimension", Integer, xDimension),
            IppAttribute("y-dimension", Integer, yDimension)
        )
    )

    override fun toString() = StringBuilder().run {
        append("${xDimension}x${yDimension}")
        toString()
    }

    fun equalsByDimensions(other: MediaSize) =
        compareByDimensions.compare(this, other) == 0

    companion object {
        fun fromIppCollection(ippCollection: IppCollection) = ippCollection.run {
            MediaSize(
                getValue("x-dimension"),
                getValue("y-dimension")
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