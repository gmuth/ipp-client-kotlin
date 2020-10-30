package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.*

class IppMedia {

    // unit: 1/100 mm, e.g. 2540 = 1 inch
    class Size(val xDimension: Int, val yDimension: Int) : IppAttributeBuilder {

        override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
                IppAttribute("media-size", IppTag.BegCollection, IppCollection(
                        IppAttribute("x-dimension", IppTag.Integer, xDimension),
                        IppAttribute("y-dimension", IppTag.Integer, yDimension)
                ))
    }

    class Margins(left: Int? = null, right: Int? = null, top: Int? = null, bottom: Int? = null) : ArrayList<IppAttribute<*>>() {
        constructor(margin: Int) : this(margin, margin, margin, margin)

        init {
            if (top != null) add(IppAttribute("media-top-margin", IppTag.Integer, top))
            if (left != null) add(IppAttribute("media-left-margin", IppTag.Integer, left))
            if (right != null) add(IppAttribute("media-right-margin", IppTag.Integer, right))
            if (bottom != null) add(IppAttribute("media-bottom-margin", IppTag.Integer, bottom))
        }
    }

    // PWG 5100.3, 3.13
    class Collection(
            var size: Size? = null,
            var margins: Margins? = null,
            var source: String? = null,
            var type: String? = null

    ) : IppAttributeBuilder {

        override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
                IppAttribute("media-col", IppTag.BegCollection, IppCollection().apply {
                    source?.let { add(IppAttribute("media-source", IppTag.Keyword, it)) }
                    type?.let { add(IppAttribute("media-type", IppTag.Keyword, it)) }
                    size?.let { add(it.buildIppAttribute(printerAttributes)) }
                    margins?.let { addAll(it) }
                })
    }

}