package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection
import de.gmuth.ipp.core.IppTag.BegCollection
import de.gmuth.ipp.core.IppTag.NameWithoutLanguage

// https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobext21-20230210-5100.7.pdf 6.3
@SuppressWarnings("kotlin:S1192")
data class MediaCollection(
    var size: MediaSize? = null,
    var margin: MediaMargin? = null,
    var source: MediaSource? = null,
    var type: String? = null, // Job Template,media-col,media-type,,type2 keyword | name(MAX),[PWG5100.7]
    var duplexSupported: Boolean? = null

) : IppAttributeBuilder {

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> {
        val mediaSize = size // conflict with IppCollection.size
        return IppAttribute("media-col", BegCollection, IppCollection().apply {
            type?.let { addAttribute("media-type", NameWithoutLanguage, it) }
            mediaSize?.let { add(it.buildIppAttribute(printerAttributes)) }
            source?.let { add(it.buildIppAttribute(printerAttributes)) }
            margin?.let { addAll(it.buildIppAttributes()) } // add up to 4 attributes
        })
    }

    override fun toString() = StringBuilder("MEDIA").run {
        size?.let { append(" size=$it") }
        margin?.let { append(" margin=$it") }
        source?.let { append(" source=$it") }
        duplexSupported?.let { append(" duplex=$it") }
        type?.let { append(" type=$it") }
    }.toString()

    fun sizeEqualsByDimensions(mediaSize: MediaSize) =
        size?.equalsByDimensions(mediaSize) ?: false

    companion object {
        fun fromIppCollection(mediaIppCollection: IppCollection) = mediaIppCollection.run {
            MediaCollection().apply {
                if (containsMember("media-size")) {
                    size = MediaSize.fromIppCollection(getValue("media-size"))
                }
                if (
                    members
                        .map { it.name }
                        .any { it.contains("margin") }
                ) {
                    margin = MediaMargin.fromIppCollection(mediaIppCollection)
                }
                if (containsMember("media-source")) {
                    source = MediaSource(getValue("media-source"))
                }
                if (containsMember("media-type")) {
                    type = getStringValue("media-type")
                }
                if (containsMember("duplex-supported")) {
                    duplexSupported = getValue<Int>("duplex-supported") == 1
                }
            }
        }
    }
}