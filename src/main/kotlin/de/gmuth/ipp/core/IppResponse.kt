package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.nio.charset.Charset

class IppResponse : IppMessage {

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

    val jobGroups: Collection<IppAttributesGroup>
        get() = getAttributesGroups(Job)

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

}