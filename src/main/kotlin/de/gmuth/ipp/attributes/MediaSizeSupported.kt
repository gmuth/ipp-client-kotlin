package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection

/**
 * https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobext21-20230210-5100.7.pdf
 *
 * 6.9.50 media-size-supported (1setOf collection)
 * This REQUIRED attribute lists the supported values of the "media-size" member attribute (section 6.3.1.15).
 * Unlike the "media-size" member attribute, the "x-dimension" and "y-dimension" member attributes of
 * "media-size-supported" have a syntax of "integer(1:MAX) | rangeOflnteger(1:MAX)" to allow for arbitrary
 * ranges of sizes for custom and roll-fed media.
 *
 * Unit: 1/100 mm, e.g. 2540 = 1 inch
 */

class MediaSizeSupported(val supportedSizes: Collection<SupportedSize>) {

    companion object {
        fun fromAttributes(attributes: IppAttributesGroup) = MediaSizeSupported(
            attributes
                .getValues<List<IppCollection>>("media-size-supported")
                .map { SupportedSize.fromIppCollection(it) }
        )
    }

    fun supports(mediaSize: MediaSize) =
        supportedSizes.any { it.supports(mediaSize) }

    override fun toString() = supportedSizes.toString()

    data class SupportedSize(val xDimension: Any, val yDimension: Any) {

        companion object {
            fun fromIppCollection(ippCollection: IppCollection) = SupportedSize(
                ippCollection.getValue("x-dimension"),
                ippCollection.getValue("y-dimension")
            )
        }

        init {
            require(dimensionsAreInt() || dimensionsAreIntRange())
        }

        override fun toString() = "${xDimension}x${yDimension}"

        fun dimensionsAreInt() = // integer(1:MAX)
            xDimension is Int && yDimension is Int

        fun dimensionsAreIntRange() = // rangeOfInteger(1:MAX)
            xDimension is IntRange && yDimension is IntRange

        fun supports(mediaSize: MediaSize) =
            if (dimensionsAreInt()) {
                xDimension as Int == mediaSize.xDimension
                        && yDimension as Int == mediaSize.yDimension
            } else { // dimensionsAreIntRange()
                (xDimension as IntRange).contains(mediaSize.xDimension)
                        && (yDimension as IntRange).contains(mediaSize.yDimension)
            }
    }
}