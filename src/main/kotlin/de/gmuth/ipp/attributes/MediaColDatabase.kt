package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection
import java.util.logging.Level
import java.util.logging.Logger

/*
* https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobext20-20190816-5100.7.pdf (section 5.5.32)
*
* This attribute lists the set of pre-defined "media-col" collections available in the Printer’s media database.
* This attribute is similar to “media-col-ready” (section 5.5.34) but returns the entire set of pre-defined "media-col"
* collections known by the Printer instead of just the media loaded in the Printer.
*/

class MediaColDatabase(val mediaCollections: List<MediaCollection>) {

    companion object {
        fun fromAttributes(attributes: IppAttributesGroup) =
            fromIppCollections(attributes.getValues("media-col-database"))

        fun fromIppCollections(mediaIppCollections: List<IppCollection>) =
            MediaColDatabase(mediaIppCollections.map { MediaCollection.fromIppCollection(it) })
    }

    fun findByMediaSize(size: MediaSize) =
        mediaCollections.filter { it.sizeEqualsByDimensions(size) }

    fun findByMediaSizeNameContaining(text: String) =
        mediaCollections.filter { it.sizeName?.contains(text) ?: false }

    fun findByMediaKeyContaining(text: String) =
        mediaCollections.filter { it.key?.contains(text) ?: false }

    val distinctMediaKeys: List<String>
        get() = mediaCollections.mapNotNull { it.key }.distinct().toList()

    val distinctMediaTypes: List<String>
        get() = mediaCollections.mapNotNull { it.type }.distinct().toList()

    val distinctMediaSources: List<MediaSource>
        get() = mediaCollections.mapNotNull { it.source }.distinct().toList()

    val distinctMediaSizes: List<MediaSize>
        get() = mediaCollections.mapNotNull { it.size }.distinct().toList()

    override fun toString() = StringBuilder("MEDIA-COL-DATABASE:").apply {
        append(" ${mediaCollections.size} definitions")
        append(", ${distinctMediaSources.size} distinct sources")
        append(", ${distinctMediaSizes.size} distinct sizes")
        append(", ${distinctMediaTypes.size} distinct types")
        append(", ${distinctMediaKeys.size} distinct keys")
    }.toString()

    @JvmOverloads
    fun log(logger: Logger, level: Level = Level.INFO) {
        logger.log(level, toString())
        distinctMediaSources.run {
            if (isNotEmpty()) logger.log(level, "media-sources:")
            forEach { logger.log(level, " $it") }
        }
        distinctMediaSizes.run {
            if (isNotEmpty()) logger.log(level, "media-sizes:")
            forEach { logger.log(level, " $it") }
        }
        distinctMediaTypes.run {
            if (isNotEmpty()) logger.log(level, "media-types:")
            forEach { logger.log(level, " $it") }
        }
        distinctMediaKeys.run {
            if (isNotEmpty()) logger.log(level, "media-keys:")
            forEach { logger.log(level, " $it") }
        }
    }
}