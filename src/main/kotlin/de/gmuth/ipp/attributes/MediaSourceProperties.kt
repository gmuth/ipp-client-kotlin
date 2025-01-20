package de.gmuth.ipp.attributes

import de.gmuth.ipp.core.IppCollection

/**
 * Copyright (c) 2025 Gerhard Muth
 */

data class MediaSourceProperties(val feedOrientation: Orientation, val feedDirection: String) {

    override fun toString() = "{feed-orientation=${feedOrientation.name.lowercase()}, feed-direction=$feedDirection}"

    companion object {
        fun fromIppCollection(ippCollection: IppCollection) = MediaSourceProperties(
            Orientation.fromInt(ippCollection.getValue("media-source-feed-orientation")),
            ippCollection.getValue("media-source-feed-direction")
        )
    }
}