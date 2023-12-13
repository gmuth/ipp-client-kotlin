package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppCollection

/**
 * This attribute lists the supported values of the "media-size" member attribute (section 5.3.1.15).
 * Unlike the "media-size" member attribute, the "x-dimension" and "y- dimension" member attributes of
 * "media-size-supported" have a syntax of "integer(1:MAX) | rangeOfInteger(1:MAX)" to allow for arbitrary
 * ranges of sizes for custom and roll-fed media.
 *
 * Unit: 1/100 mm, e.g. 2540 = 1 inch
 */
data class MediaSizeSupported(val xDimension: Any, val yDimension: Any) {

    init {
        require(dimensionsAreInt() || dimensionsAreIntRange())
    }

    override fun toString() = "$xDimension x $yDimension"

    fun dimensionsAreInt() = // integer(1:MAX)
        xDimension is Int && yDimension is Int

    fun dimensionsAreIntRange() = // rangeOfInteger(1:MAX)
        xDimension is IntRange && yDimension is IntRange

    fun toMediaSize() =
        if (dimensionsAreInt()) MediaSize(xDimension as Int, yDimension as Int)
        else error("dimensions are not Int")

    companion object {
        fun fromIppCollection(ippCollection: IppCollection) = ippCollection.run {
            MediaSizeSupported(
                getValue("x-dimension"),
                getValue("y-dimension")
            )
        }
    }
}