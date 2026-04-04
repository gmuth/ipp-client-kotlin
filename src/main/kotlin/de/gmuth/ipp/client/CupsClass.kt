package de.gmuth.ipp.client

/**
 * Copyright (c) 2026 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger

class CupsClass(
    attributes: IppAttributesGroup,
    ippClient: IppClient
) :
    IppPrinter(
        printerUri = attributes.getValues<List<URI>>("printer-uri-supported").first(),
        attributes = attributes,
        ippClient = ippClient
    ) {

    class Member(
        val name: String,
        val uri: URI
    ) {
        override fun toString() = "$name, $uri"
    }

    val members: List<Member>
        get() = attributes.let {
            val memberNamesList = it.getValues<List<IppString>>("member-names")
            val memberUrisList = it.getValues<List<URI>>("member-uris")
            memberUrisList.indices.map { Member(name = memberNamesList[it].text, uri = memberUrisList[it]) }
        }

    override fun toString() =
        "Class: $name (${members.size} members), ${communicationChannelsSupported.first()}"

    override fun log(logger: Logger, level: Level) {
        logger.info { super.toString() }
        logger.info { toString() }
        members.forEach { logger.info { "- $it" } }
    }
}