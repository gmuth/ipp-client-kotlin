package de.gmuth.ipp.client

/**
 * Copyright (c) 2026 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppOperation.CupsAddModifyClass
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.core.IppTag.Printer
import de.gmuth.ipp.core.IppTag.Uri
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

class CupsPrinterClass(
    classUri: URI,
    attributes: IppAttributesGroup = IppAttributesGroup(Printer),
    private val cupsClient: CupsClient
) :
    IppPrinter(
        printerUri = classUri,
        attributes = attributes,
        ippClient = cupsClient.ippClient
    ) {
    constructor(
        attributes: IppAttributesGroup,
        cupsClient: CupsClient
    ) : this(
        classUri = attributes.getValues<List<URI>>("printer-uri-supported").first(),
        attributes = attributes,
        cupsClient = cupsClient
    )

    class Member(
        val printerClass: CupsPrinterClass,
        val name: String,
        val uri: URI
    ) {
        val printer: IppPrinter by lazy { printerClass.cupsClient.getPrinter(name) }
        override fun toString() = "$name, $uri"
    }

    private val memberUris: List<URI>
        get() = attributes.getValues("member-uris")

    private val memberNames: List<IppString>
        get() = attributes.getValues("member-names")

    val members: List<Member>
        get() = memberUris.indices.map {
            Member(
                printerClass = this,
                name = memberNames[it].text,
                uri = memberUris[it]
            )
        }

    private val logger = getLogger(javaClass.name)

    override fun toString() =
        "PrinterClass $name (${members.size} members), ${communicationChannelsSupported.first()}"

    fun addModifyClass(printerUris: Collection<URI>) = exchange(
        ippRequest(CupsAddModifyClass).apply {
            createAttributesGroup(Printer).apply {
                if (printerUris.isEmpty()) throw IppException("printerUris is empty.")
                attribute("member-uris", Uri, printerUris.toSet())
            }
        }
    )

    private fun updateMemberAttrbites() =
        updateAttributes("member-uris", "member-names")

    fun addMembers(printerUris: Collection<URI>) =
        addModifyClass(memberUris + printerUris).apply {
            updateMemberAttrbites()
            logger.info { "Added members: ${printerUris.joinToString(", ")}" }
        }

    fun addMember(printerUri: URI) =
        addMembers(listOf(printerUri))

    fun removeMembers(printerUris: Collection<URI>) =
        addModifyClass(memberUris - printerUris).apply {
            updateMemberAttrbites()
            logger.info { "Removed members: ${printerUris.joinToString(", ")}" }
        }

    fun removeMember(printerUri: URI) =
        removeMembers(listOf(printerUri))

    override fun log(logger: Logger, level: Level) {
        logger.log(level) { toString() }
        members.forEach { logger.log(level) { "- $it" } }
    }
}