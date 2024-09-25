package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.net.URI
import java.nio.charset.Charset
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger

class IppRequest : IppMessage {
    private val logger = Logger.getLogger(javaClass.name)

    val printerOrJobUri: URI
        @SuppressWarnings("kotlin:S1192")
        get() = operationGroup.run {
            when {
                containsKey("printer-uri") -> getValueAsURI("printer-uri")
                containsKey("job-uri") -> getValueAsURI("job-uri")
                else -> throw IppException("Missing 'printer-uri' or 'job-uri' in IppRequest")
                    .also { log(logger, Level.WARNING) }
            }
        }

    override val codeDescription: String
        get() = operation.toString()

    val operation: IppOperation
        get() = IppOperation.fromInt(code!!)

    val requestedAttributes: List<String>
        get() = operationGroup.getValues("requested-attributes")

    constructor() : super()

    constructor(
        operation: IppOperation,
        printerUri: URI? = null,
        requestedAttributes: Collection<String>? = null,
        requestingUserName: String? = null,
        version: String = "2.0",
        requestId: Int = 1,
        charset: Charset = Charsets.UTF_8,
        naturalLanguage: String = "en-us"
    ) : super(version, requestId, charset, naturalLanguage) {
        code = operation.code
        operationGroup.run {
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