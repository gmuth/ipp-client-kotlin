package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.*

// PWG 5100.3, 3.13

class IppMediaCollection(
        var size: Size? = null,
        var margin: Margin? = null,
        var source: String? = null,
        var type: String? = null

) : IppAttributeBuilder {

    // unit: 1/100 mm, e.g. 2540 = 1 inch
    class Size(val xDimension: Int, val yDimension: Int) : IppAttributeBuilder {
        override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
                IppAttribute("media-size", IppTag.BegCollection, IppCollection(
                        IppAttribute("x-dimension", IppTag.Integer, xDimension),
                        IppAttribute("y-dimension", IppTag.Integer, yDimension)
                ))
    }

    class Margin(val left: Int? = null, val right: Int? = null, val top: Int? = null, val bottom: Int? = null) {
        constructor(margin: Int) : this(margin, margin, margin, margin)

        fun getIppAttributes(): Collection<IppAttribute<*>> {
            with(mutableListOf<IppAttribute<*>>()) {
                if (top != null) add(IppAttribute("media-top-margin", IppTag.Integer, top))
                if (left != null) add(IppAttribute("media-left-margin", IppTag.Integer, left))
                if (right != null) add(IppAttribute("media-right-margin", IppTag.Integer, right))
                if (bottom != null) add(IppAttribute("media-bottom-margin", IppTag.Integer, bottom))
                return this
            }
        }
    }

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        with(IppCollection()) {
            if (source != null) add(IppAttribute("media-source", IppTag.Keyword, source))
            if (type != null) add(IppAttribute("media-type", IppTag.Keyword, type))
            if (size != null) add(size!!.buildIppAttribute(printerAttributes))
            margin?.getIppAttributes()?.forEach { add(it) }
            return IppAttribute("media-col", IppTag.BegCollection, this)
        }
    }

}