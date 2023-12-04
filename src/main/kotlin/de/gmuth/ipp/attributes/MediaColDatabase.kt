package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection
import java.util.logging.Level
import java.util.logging.Logger

class MediaColDatabase(val mediaCollections: List<MediaCollection>) {
    companion object {
        fun fromAttributes(attributes: IppAttributesGroup) = MediaColDatabase(
            attributes
                .getValues<List<IppCollection>>("media-col-database")
                .map { MediaCollection.fromIppCollection(it) }
        )
    }

    fun findWithSizeNameContaining(text: String) =
        mediaCollections.filter { it.size?.name?.contains(text) ?: false }

    val distinctMediaTypes: List<String>
        get() = mediaCollections.mapNotNull { it.type }.distinct().toList()

    val distinctMediaSources: List<MediaSource>
        get() = mediaCollections.mapNotNull { it.source }.distinct().toList()

    val distinctMediaSizes: List<MediaSize>
        get() = mediaCollections.mapNotNull { it.size }.distinct().toList()

    override fun toString() = StringBuilder("MEDIA-COL-DATABASE:").run {
        append(" ${mediaCollections.size} definitions")
        append(", ${distinctMediaSources.size} distinct sources")
        append(", ${distinctMediaSizes.size} distinct sizes")
        append(", ${distinctMediaTypes.size} distinct types")
    }.toString()

    @JvmOverloads
    fun log(logger: Logger, level: Level = Level.INFO) {
        logger.log(level, toString())
        logger.log(level, "media-sources:")
        distinctMediaSources.forEach { logger.log(level, " $it") }
        logger.log(level, "media-sizes:")
        distinctMediaSizes.forEach { logger.log(level, " $it") }
        logger.log(level, "media-types:")
        distinctMediaTypes.forEach { logger.log(level, " $it") }
    }
}