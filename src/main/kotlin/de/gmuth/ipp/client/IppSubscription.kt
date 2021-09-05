package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag.Integer
import de.gmuth.ipp.core.IppTag.Subscription
import de.gmuth.log.Logging

class IppSubscription(
        val printer: IppPrinter,
        var attributes: IppAttributesGroup
) {
    companion object {
        val log = Logging.getLogger {}
    }

    val id: Int
        get() = attributes.getValue("notify-subscription-id")

    val events: List<String>
        get() = attributes.getValues("notify-events")

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
                operationGroup.attribute("notify-subscription-id", Integer, id)
            }

    fun exchange(request: IppRequest) =
            printer.exchange(request)

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder("subscription #$id").apply {
        if (attributes.contains("notify-events")) append(": ${events.joinToString(",")}")
    }.toString()

    fun logDetails() {
        attributes.logDetails(title = "subscription #$id")
    }

}