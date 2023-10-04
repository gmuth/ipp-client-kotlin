package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppTag.Integer

open class MediaMargin(
    val left: Int? = null,
    val right: Int? = null,
    val top: Int? = null,
    val bottom: Int? = null
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
}