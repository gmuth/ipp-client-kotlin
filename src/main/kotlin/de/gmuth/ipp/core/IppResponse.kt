package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.Operation
import de.gmuth.ipp.core.IppTag.Unsupported
import java.nio.charset.Charset
import java.util.logging.Level
import java.util.logging.Logger

class IppResponse : IppMessage {

    var httpServer: String? = null

    override val codeDescription: String
        get() = status.toString()

    var status: IppStatus
        get() = IppStatus.fromInt(code!!)
        set(ippStatus) {
            code = ippStatus.code
        }

    // https://datatracker.ietf.org/doc/html/rfc8011#page-42
    val statusMessage: IppString
        get() = operationGroup.getValue("status-message")

    val unsupportedGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(Unsupported)

    fun isSuccessful() = status.isSuccessful()

    constructor() : super()

    constructor(
        status: IppStatus,
        version: String = "2.0",
        requestId: Int = 1,
        charset: Charset = Charsets.UTF_8,
        naturalLanguage: String = "en"
    ) : super(version, requestId, charset, naturalLanguage) {
        code = status.code
    }

    override fun log(logger: Logger, level: Level, prefix: String) {
        httpServer?.let { logger.log(level) { "${prefix}httpServer = $it" } }
        super.log(logger, level, prefix)
    }

    override fun toString() = StringBuilder().apply {
        append(status)

        val statesAndReasons = attributesGroups
            .flatMap { group -> group.values }
            .filter { attribute -> Regex(".*-state(-reasons)?").matches(attribute.name) }
            .sortedBy { it.name }
            .map { it.valuesToString() }
            .filter { it.isNotEmpty() && it != "none" }
        if (statesAndReasons.isNotEmpty())
            append(statesAndReasons.joinToString(", ", " [", "]"))

        val groups = attributesGroups
            .filter { group -> group.tag != Operation }
            .map { "${it.size} ${it.tag.name.lowercase()} attributes" }
        if (groups.isNotEmpty())
            append(groups.joinToString(", ", " (", ")"))

    }.toString()
}