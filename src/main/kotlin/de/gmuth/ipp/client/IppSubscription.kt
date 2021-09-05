package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.core.IppTag.Integer
import de.gmuth.ipp.core.IppTag.Subscription
import de.gmuth.log.Logging
import java.net.URI

@SuppressWarnings("kotlin:S1192")
class IppSubscription(
        val printer: IppPrinter,
        var attributes: IppAttributesGroup
) {
    companion object {
        val log = Logging.getLogger { }
    }

    val notifySubscriptionId: Int
        get() = attributes.getValue("notify-subscription-id")

    val notifyPrinterUri: URI
        get() = attributes.getValue("notify-printer-uri")

    val notifyEvents: List<String>
        get() = attributes.getValues("notify-events")

    val notifyLeaseDuration: Int
        get() = attributes.getValue("notify-lease-duration")

    val notifyPullMethod: String
        get() = attributes.getValue("notify-pull-method")

    val notifySubscriberUserName: IppString
        get() = attributes.getValue("notify-subscriber-user-name")

    val notifyTimeInterval: Int
        get() = attributes.getValue("notify-time-interval")

    //----------------------------
    // get subscription attributes
    //----------------------------

    // RFC 3995 11.2.4.1.2: 'subscription-template', 'subscription-description' or  'all' (default)

    @JvmOverloads
    fun getSubscriptionAttributes(requestedAttributes: List<String>? = null) =
            exchange(ippRequest(GetSubscriptionAttributes, requestedAttributes = requestedAttributes))

    fun updateAllAttributes() {
        attributes = getSubscriptionAttributes().getSingleAttributesGroup(Subscription)
    }

    //---------------
    // administration
    //----------------

    fun cancel() =
            exchange(ippRequest(CancelSubscription))

    fun renew(notifyLeaseDuration: Int? = null) =
            exchange(ippRequest(RenewSubscription).apply {
                createAttributesGroup(Subscription).apply {
                    notifyLeaseDuration?.let { attribute("notify-lease-duration", Integer, it) }
                }
            }).also { updateAllAttributes() }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    fun ippRequest(operation: IppOperation, requestedAttributes: List<String>? = null) =
            printer.ippRequest(operation, requestedAttributes = requestedAttributes).apply {
                operationGroup.attribute("notify-subscription-id", Integer, notifySubscriptionId)
            }

    fun exchange(request: IppRequest) =
            printer.exchange(request)

    // -------
    // Logging
    // -------

    override fun toString() =
            "subscription #$notifySubscriptionId: ${notifyEvents.joinToString(",")}"

    fun logDetails() {
        log.info { "subscription id:   $notifySubscriptionId" }
        log.info { "printer uri:       $notifyPrinterUri" }
        log.info { "events:            ${notifyEvents.joinToString(",")}" }
        log.info { "subscriber user:   $notifySubscriberUserName" }
        if (attributes.containsKey("notify-lease-duration")) log.info { "lease duration:    $notifyLeaseDuration seconds" }
        if (attributes.containsKey("notify-time-interval")) log.info { "time interval:     $notifyTimeInterval" }
        if (attributes.containsKey("notify-pull-method")) log.info { "pull method:       $notifyPullMethod" }
    }

}