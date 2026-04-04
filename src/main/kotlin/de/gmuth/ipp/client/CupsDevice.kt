package de.gmuth.ipp.client

/**
 * Copyright (c) 2026 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString
import java.net.URI

class CupsDevice(
    val id: String,
    val uri: URI,
    val info: String,
    val deviceClass: String,
    val location: String,
    val makeAndModel: String
) {
    constructor(attributes: IppAttributesGroup) : this(
        attributes.getValue<IppString>("device-id").text,
        attributes.getValueAsURI("device-uri"),
        attributes.getValue<IppString>("device-info").text,
        attributes.getKeywordOrName("device-class"),
        attributes.getValue<IppString>("device-location").text,
        attributes.getValue<IppString>("device-make-and-model").text
    )

    override fun toString() = StringBuilder().run {
        append(info)
        append(", $makeAndModel")
        append(", class=\"$deviceClass\"")
        append(", uri=\"$uri\"")
        if (location.isNotEmpty()) append(", location=$location")
        if (id.isNotEmpty()) append(", id=\"$id\"")
        toString()
    }

}