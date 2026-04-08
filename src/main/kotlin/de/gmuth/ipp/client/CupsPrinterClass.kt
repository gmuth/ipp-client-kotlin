package de.gmuth.ipp.client

/**
 * Copyright (c) 2026 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation.CupsAddModifyClass
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.core.IppTag.Printer
import de.gmuth.ipp.core.IppTag.Uri
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

@Suppress("kotlin:S1192")
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
        val printer: IppPrinter by lazy { IppPrinter(uri, ippClient = printerClass.ippClient) }
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
        "PrinterClass $name ($state, ${members.size} members), ${communicationChannelsSupported.joinToString(", ")}"

    fun addModifyClass(memberUris: Collection<URI>) = exchange(
        ippRequest(CupsAddModifyClass).apply {
            createAttributesGroup(Printer).apply {
                require(memberUris.isNotEmpty(), { "memberUris must not be empty." })
                attribute("member-uris", Uri, memberUris.toSet())
            }
        }
    )

    fun updateMemberAttributes() =
        updateAttributes("member-uris", "member-names")

    fun addMembers(printerUris: Collection<URI>) =
        addModifyClass(memberUris + printerUris).apply {
            logger.info { "Added members: ${printerUris.joinToString(", ")}" }
            updateMemberAttributes()
        }

    fun addMember(printerUri: URI) = addMembers(listOf(printerUri))
    fun addMember(printer: IppPrinter) = addMember(printer.printerUri)

    fun removeMembers(printerUris: Collection<URI>) =
        addModifyClass(memberUris - printerUris).apply {
            logger.info { "Removed members: ${printerUris.joinToString(", ")}" }
            updateMemberAttributes()
        }

    fun removeMember(printerUri: URI) = removeMembers(listOf(printerUri))
    fun removeMember(printer: IppPrinter) = removeMember(printer.printerUri)

    fun delete() = cupsClient.run {
        getJobs(WhichJobs.NotCompleted).run {
            if (isNotEmpty()) logger.warning { "Printer class $name has $size not completed jobs" }
        }
        deleteClass(name.text)
    }

    override fun log(logger: Logger, level: Level) {
        logger.log(level) { toString() }
        members.forEach { logger.log(level) { "- $it" } }
    }
}