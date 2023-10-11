package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.net.URI
import java.nio.charset.Charset
import java.time.Duration

class IppRequest : IppMessage {

    val printerUri: URI
        get() = operationGroup.getValueOrNull("printer-uri") ?: throw IppException("Missing 'printer-uri'")

    override val codeDescription: String
        get() = operation.toString()

    val operation: IppOperation
        get() = IppOperation.fromInt(code!!)

    val attributesCharset: Charset
        get() = operationGroup.getValue("attributes-charset")

    val requestingUserName: String
        get() = operationGroup.getTextValue("requesting-user-name")

    constructor() : super()

    constructor(
        operation: IppOperation,
        printerUri: URI? = null,
        jobId: Int? = null,
        requestedAttributes: Collection<String>? = null,
        requestingUserName: String? = null,
        version: String = "2.0",
        requestId: Int = 1,
        charset: Charset = Charsets.UTF_8,
        naturalLanguage: String = "en"
    ) : super(version, requestId, charset, naturalLanguage) {
        code = operation.code
        operationGroup.run {
            jobId?.let { attribute("job-id", Integer, it) }
            printerUri?.let { attribute("printer-uri", Uri, it) }
            requestedAttributes?.let { attribute("requested-attributes", Keyword, it) }
            requestingUserName?.let { attribute("requesting-user-name", NameWithoutLanguage, IppString(it)) }
        }
    }

    fun createSubscriptionAttributesGroup(
        notifyEvents: Collection<String>? = null,
        notifyLeaseDuration: Duration? = null,
        notifyTimeInterval: Duration? = null,
        notifyJobId: Int? = null
    ) = createAttributesGroup(Subscription).apply {
        attribute("notify-pull-method", Keyword, "ippget")
        notifyJobId?.let { attribute("notify-job-id", Integer, it) }
        notifyEvents?.let { attribute("notify-events", Keyword, it) }
        notifyTimeInterval?.let { attribute("notify-time-interval", Integer, it.toMillis() / 1000) }
        notifyLeaseDuration?.let { attribute("notify-lease-duration", Integer, it.toMillis() / 1000) }
    }

}