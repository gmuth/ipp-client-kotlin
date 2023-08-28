package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

import de.gmuth.ipp.client.IppExchangeException.ClientErrorNotFoundException
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging
import java.time.Duration
import java.time.Duration.ofSeconds
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class IppSubscription(
    val printer: IppPrinter,
    var attributes: IppAttributesGroup
) {
    companion object {
        val log = Logging.getLogger {}
    }

    private var lastSequenceNumber: Int = 0
    private var leaseStartedAt = now()

    init {
        if (attributes.size <= 1) {
            updateAttributes()
            log.info { toString() }
        }
    }

    val id: Int
        get() = attributes.getValue("notify-subscription-id")

    val leaseDuration: Duration
        get() = ofSeconds(attributes.getValue<Int>("notify-lease-duration").toLong())

    val events: List<String>
        get() = attributes.getValues("notify-events")

    val jobId: Int
        get() = attributes.getValue("notify-job-id")

    val subscriberUserName: IppString
        get() = attributes.getValue("notify-subscriber-user-name")

    val timeInterval: Duration
        get() = ofSeconds(attributes.getValue<Int>("notify-time-interval").toLong())

    fun hasJobId() = attributes.containsKey("notify-job-id")

    //----------------------------
    // Get-Subscription-Attributes
    //----------------------------

    // RFC 3995 11.2.4.1.2: 'subscription-template', 'subscription-description' or 'all' (default)

    @JvmOverloads
    fun getSubscriptionAttributes(requestedAttributes: List<String>? = null) =
        exchange(ippRequest(GetSubscriptionAttributes, requestedAttributes = requestedAttributes))

    fun updateAttributes() {
        attributes = getSubscriptionAttributes().getSingleAttributesGroup(Subscription)
    }

    //------------------
    // Get-Notifications
    //------------------

    fun getNotifications(notifySequenceNumber: Int? = lastSequenceNumber + 1): List<IppEventNotification> {
        val request = ippRequest(GetNotifications).apply {
            operationGroup.run {
                attribute("notify-subscription-ids", Integer, id)
                notifySequenceNumber?.let { attribute("notify-sequence-numbers", Integer, it) }
            }
        }
        return exchange(request)
            .getAttributesGroups(EventNotification)
            .map { IppEventNotification(this, it) }
            .apply { if (isNotEmpty()) lastSequenceNumber = last().sequenceNumber }
    }

    //--------------------
    // Cancel-Subscription
    //--------------------

    fun cancel() = exchange(ippRequest(CancelSubscription))

    //-------------------
    // Renew-Subscription
    //-------------------

    fun renew(leaseDuration: Duration? = null) =
        exchange(ippRequest(RenewSubscription).apply {
            createSubscriptionAttributesGroup(notifyLeaseDuration = leaseDuration)
        }).also {
            leaseStartedAt = now()
            updateAttributes()
            log.info { "renewed $this" }
        }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    fun ippRequest(operation: IppOperation, requestedAttributes: List<String>? = null) =
        printer.ippRequest(operation, requestedAttributes = requestedAttributes).apply {
            operationGroup.attribute("notify-subscription-id", Integer, id)
        }

    fun exchange(request: IppRequest) = printer.exchange(request)

    //------------------------------------
    // poll and handle event notifications
    //------------------------------------

    var pollHandlesNotifications = false

    val expiresAt: LocalDateTime
        get() = leaseStartedAt.plus(leaseDuration)

    fun expired() = !leaseDuration.isZero && now().isAfter(expiresAt)

    fun pollAndHandleNotifications(
        pollEvery: Duration = ofSeconds(5), // should be larger than 1s
        autoRenewSubscription: Boolean = false,
        handleNotification: (event: IppEventNotification) -> Unit = { log.info { it } }
    ) {
        fun expiresAfterDelay() = !leaseDuration.isZero && now().plus(pollEvery).isAfter(expiresAt.minusSeconds(2))
        try {
            pollHandlesNotifications = true
            do {
                if (expired()) log.warn { "subscription #$id has expired" }
                getNotifications().forEach { handleNotification(it) }
                if (expiresAfterDelay() && autoRenewSubscription) renew(leaseDuration)
                Thread.sleep(pollEvery.toMillis())
            } while (pollHandlesNotifications)
        } catch (clientErrorNotFoundException: ClientErrorNotFoundException) {
            log.info { clientErrorNotFoundException.response!!.statusMessage }
        }
    }

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder("subscription #$id:").run {
        if (hasJobId()) append(" job #$jobId")
        if (attributes.containsKey("notify-events")) append(" events=${events.joinToString(",")}")
        if (attributes.containsKey("notify-time-interval")) append(" time-interval=$timeInterval")
        if (attributes.containsKey("notify-lease-duration")) {
            append(" lease-duration=$leaseDuration (expires at $expiresAt)")
        }
        toString()
    }

    fun logDetails() = attributes.logDetails(title = "subscription #$id")

}