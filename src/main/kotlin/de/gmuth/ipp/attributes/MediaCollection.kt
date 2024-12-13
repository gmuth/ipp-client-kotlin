package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection
import de.gmuth.ipp.core.IppTag.BegCollection
import de.gmuth.ipp.core.IppTag.NameWithoutLanguage
import java.util.logging.Logger

// https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobext21-20230210-5100.7.pdf 6.3
@SuppressWarnings("kotlin:S1192")
data class MediaCollection(
    var size: MediaSize? = null,
    var margin: MediaMargin? = null,
    var source: MediaSource? = null,
    var type: String? = null, // Job Template,media-col,media-type,,type2 keyword | name(MAX),[PWG5100.7]
    var duplexSupported: Boolean? = null

) : IppAttributeBuilder {

    private val logger = Logger.getLogger(javaClass.name)

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        val mediaSize = size // conflict with IppCollection.size
        return IppAttribute("media-col", BegCollection, IppCollection().apply {
            type?.let { addAttribute("media-type", NameWithoutLanguage, it) }
            mediaSize?.let { add(it.buildIppAttribute(printerAttributes)) }
            source?.let { add(it.buildIppAttribute(printerAttributes)) }
            margin?.let { addAll(it.buildIppAttributes()) } // add up to 4 attributes
        })
    }

    override fun toString() = StringBuilder("MEDIA").apply {
        size?.let { append(" size=$it") }
        margin?.let { append(" margin=$it") }
        source?.let { append(" source=$it") }
        duplexSupported?.let { append(" duplex=$it") }
        type?.let { append(" type=$it") }
    }.toString()

    fun sizeEqualsByDimensions(mediaSize: MediaSize) =
        size?.equalsByDimensions(mediaSize) ?: false

    companion object {
        fun fromIppCollection(mediaIppCollection: IppCollection) = MediaCollection().apply {
            for (member in mediaIppCollection.members) with(member) {
                when (name) {
                    "media-size" -> size = MediaSize.fromIppCollection(value as IppCollection)
                    "media-type" -> type = getKeywordOrName()
                    "media-source" -> source = MediaSource(getKeywordOrName())
                    "duplex-supported" -> duplexSupported = (value as Int) == 1
                    else -> if (!isMediaMargin()) logger.warning { "unsupported member: $member" }
                }
            }
            if (mediaIppCollection.members.any { it.isMediaMargin() }) {
                margin = MediaMargin.fromIppCollection(mediaIppCollection)
            }
        }
    }

    private fun IppAttribute<*>.isMediaMargin() = Regex("media-.*-margin").matches(name)
}