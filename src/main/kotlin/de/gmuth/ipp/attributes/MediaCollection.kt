package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.*
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

    companion object {
        fun fromIppCollection(mediaIppCollection: IppCollection) = mediaIppCollection.run {
            MediaCollection().apply {
                // media-size
                if (containsMember("media-size")) {
                    size = MediaSize.fromIppCollection(getValue("media-size"))
                }
                // media-top-margin, media-bottom-margin, media-left-margin, media-right-margin
                if (
                    members
                        .map { it.name }
                        .any { it.contains("margin") }
                ) {
                    margin = MediaMargin.fromIppCollection(mediaIppCollection)
                }
                // media-source
                if (containsMember("media-source")) {
                    source = MediaSource(getValue("media-source"))
                }
                // media-type
                if (containsMember("media-type")) {
                    type = getValue<IppString>("media-type").text
                }
                // duplex-supported
                if (containsMember("duplex-supported")) {
                    duplexSupported = getValue<Int>("duplex-supported") == 1
                }
            }
        }
    }
}