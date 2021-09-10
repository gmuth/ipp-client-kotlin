package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging

class IppSubscription(
        val printer: IppPrinter,
        var attributes: IppAttributesGroup,
        updateAttributes: Boolean = attributes.size <= 1
) {
    companion object {
        val log = Logging.getLogger {}
    }

    init {
        if (updateAttributes) updateAllAttributes()
    }

    private var lastSequenceNumber: Int = 0

    val id: Int
        get() = attributes.getValue("notify-subscription-id")

    val events: List<String>
        get() = attributes.getValues("notify-events")

    val jobId: Int
        get() = attributes.getValue("notify-job-id")

    fun hasJobId() = attributes.containsKey("notify-job-id")

    //----------------------------
    // Get-Subscription-Attributes
    //----------------------------

    // RFC 3995 11.2.4.1.2: 'subscription-template', 'subscription-description' or  'all' (default)

    @JvmOverloads
    fun getSubscriptionAttributes(requestedAttributes: List<String>? = null) =
            exchange(ippRequest(GetSubscriptionAttributes, requestedAttributes = requestedAttributes))

    fun updateAllAttributes() {
        attributes = getSubscriptionAttributes().getSingleAttributesGroup(Subscription)
    }

    //------------------
    // Get-Notifications
    //------------------

    fun getNotifications(
            onlyNewEvents: Boolean = false,
            notifySequenceNumber: Int? = if (onlyNewEvents) lastSequenceNumber + 1 else null
    ): List<IppEventNotification> {
        val request = ippRequest(GetNotifications).apply {
            operationGroup.run {
                attribute("notify-subscription-ids", Integer, id)
                notifySequenceNumber?.let { attribute("notify-sequence-numbers", Integer, it) }
            }
        }
        return exchange(request)
                .getAttributesGroups(EventNotification)
                .map { IppEventNotification(it) }
                .apply { if (isNotEmpty()) lastSequenceNumber = last().sequenceNumber }
    }

    //--------------------
    // Cancel-Subscription
    //--------------------

    fun cancel() = exchange(ippRequest(CancelSubscription))

    //-------------------
    // Renew-Subscription
    //-------------------

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

    fun exchange(request: IppRequest) = printer.exchange(request)

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder("subscription #$id:").run {
        if (hasJobId()) append(" job #$jobId")
        if (attributes.containsKey("notify-events")) append(" events=${events.joinToString(",")}")
        toString()
    }

    fun logDetails() = attributes.logDetails(title = "subscription #$id")

}