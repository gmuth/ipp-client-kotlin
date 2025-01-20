package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppTag.BegCollection
import de.gmuth.ipp.core.IppTag.NameWithoutLanguage
import java.util.logging.Logger

// https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobext21-20230210-5100.7.pdf 6.3
@SuppressWarnings("kotlin:S1192")
data class MediaCollection(
    var size: MediaSize? = null,
    var margin: MediaMargin? = null,
    var source: MediaSource? = null,
    var type: String? = null, // media-type (type2 keyword | name(MAX)) [PWG5100.7]
    var sizeName: String? = null, // media-size-name (type2 keyword | name(MAX)) [PWG5100.7]
    var key: String? = null,
    var sourceProperties: MediaSourceProperties? = null,
) : IppAttributeBuilder {

    private val logger = Logger.getLogger(javaClass.name)

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        val mediaSize = size // conflict with IppCollection.size
        return IppAttribute("media-col", BegCollection, IppCollection().apply {
            sizeName?.let { addAttribute("media-size-name", NameWithoutLanguage, it) }
            type?.let { addAttribute("media-type", NameWithoutLanguage, it) }
            key?.let { addAttribute("media-key", NameWithoutLanguage, it) }
            mediaSize?.let { add(it.buildIppAttribute(printerAttributes)) }
            source?.let { add(it.buildIppAttribute(printerAttributes)) }
            margin?.let { addAll(it.buildIppAttributes()) } // add up to 4 attributes
        })
    }

    override fun toString() = StringBuilder("MEDIA").apply {
        key?.let { append(" key=$it") }
        size?.let { append(" size=$it") }
        sizeName?.let { append(" size-name=$it") }
        margin?.let { append(" margin=$it") }
        source?.let { append(" source=$it") }
        type?.let { append(" type=$it") }
        sourceProperties?.let { append(" source-properties=$it") }
    }.toString()

    fun sizeEqualsByDimensions(mediaSize: MediaSize) =
        size?.equalsByDimensions(mediaSize) ?: false

    companion object {
        fun fromIppCollection(mediaIppCollection: IppCollection) = MediaCollection().apply {
            for (member in mediaIppCollection.members) with(member) {
                when (name) {
                    "media-key" -> key = getKeywordOrName()
                    "media-size" -> setMediaSize(value as IppCollection)
                    "media-size-name" -> sizeName = getKeywordOrName()
                    "media-type" -> type = getKeywordOrName()
                    "media-source" -> source = MediaSource(getKeywordOrName())
                    "media-source-properties" -> sourceProperties =
                        MediaSourceProperties.fromIppCollection(value as IppCollection)

                    else -> if (!isMediaMargin()) logger.warning { "Ignored unsupported member: $member" }
                }
            }
            if (mediaIppCollection.members.any { it.isMediaMargin() }) {
                margin = MediaMargin.fromIppCollection(mediaIppCollection)
            }
        }
    }

    private fun MediaCollection.setMediaSize(ippCollection: IppCollection) {
        if (ippCollection.getMember<Any>("x-dimension").tag == IppTag.Integer
            && ippCollection.getMember<Any>("y-dimension").tag == IppTag.Integer)
            size = MediaSize.fromIppCollection(ippCollection)
        else
            logger.warning { "Ignored unsupported media-size: " + ippCollection }
    }

    private fun IppAttribute<*>.isMediaMargin() = Regex("media-.*-margin").matches(name)
}