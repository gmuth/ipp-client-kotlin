package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppCollection
import de.gmuth.ipp.core.IppTag.Integer

data class MediaMargin(
    var left: Int? = null,
    var right: Int? = null,
    var top: Int? = null,
    var bottom: Int? = null
) {
    constructor(margin: Int) : this(margin, margin, margin, margin)

    fun buildIppAttributes(): Collection<IppAttribute<Int>> =
        ArrayList<IppAttribute<Int>>().apply {
            fun addMargin(side: String, value: Int) = add(IppAttribute("media-$side-margin", Integer, value))
            top?.let { addMargin("top", it) }
            left?.let { addMargin("left", it) }
            right?.let { addMargin("right", it) }
            bottom?.let { addMargin("bottom", it) }
        }

    override fun toString() =
        if (listOf(top, bottom, left, right).distinct().size == 1) "$top"
        else "top=%d;bottom=%d;left=%d;right=%d".format(top, bottom, left, right)

    companion object {
        fun fromIppCollection(ippCollection: IppCollection) = ippCollection.run {
            MediaMargin().apply {
                if (containsMember("media-top-margin")) top = getValue("media-top-margin")
                if (containsMember("media-left-margin")) left = getValue("media-left-margin")
                if (containsMember("media-right-margin")) right = getValue("media-right-margin")
                if (containsMember("media-bottom-margin")) bottom = getValue("media-bottom-margin")
            }
        }
    }
}