package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.*

// PWG 5100.3, 3.13

class IppMedia(
        val size: Size? = null,
        val margins: Margins? = null,
        val source: String? = null,
        val type: String? = null

) : IppAttributeHolder {

    // unit: 1/100 mm, e.g. 2540 = 1 inch
    class Size(val xDimension: Int, val yDimension: Int) : IppAttributeHolder {
        override fun getIppAttribute(printerAttributes: IppAttributesGroup) =
                IppAttribute("media-size", IppTag.BegCollection, IppCollection(
                        IppAttribute("x-dimension", IppTag.Integer, xDimension),
                        IppAttribute("y-dimension", IppTag.Integer, yDimension)
                ))
    }

    class Margins(val left: Int?, val right: Int?, val top: Int?, val bottom: Int?) {
        constructor(margin: Int) : this(margin, margin, margin, margin)

        fun getIppAttributes(): Collection<IppAttribute<*>> {
            val marginAttributes = mutableListOf<IppAttribute<*>>()
            if (left != null) {
                marginAttributes.add(IppAttribute("media-left-margin", IppTag.Integer, left))
            }
            if (right != null) {
                marginAttributes.add(IppAttribute("media-right-margin", IppTag.Integer, right))
            }
            if (top != null) {
                marginAttributes.add(IppAttribute("media-top-margin", IppTag.Integer, top))
            }
            if (bottom != null) {
                marginAttributes.add(IppAttribute("media-bottom-margin", IppTag.Integer, bottom))
            }
            return marginAttributes
        }
    }

    override fun getIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        val mediaColAttribute = IppAttribute("media-col", IppTag.BegCollection, IppCollection())
        margins?.getIppAttributes()?.forEach { mediaColAttribute.value.add(it) }
        if (size != null) {
            mediaColAttribute.value.add(size.getIppAttribute(printerAttributes))
        }
        if (source != null) {
            mediaColAttribute.value.add(IppAttribute("media-source", IppTag.Keyword, source))
        }
        if (type != null) {
            mediaColAttribute.value.add(IppAttribute("media-type", IppTag.Keyword, type))
        }
        return mediaColAttribute
    }

}