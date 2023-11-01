package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

import de.gmuth.ipp.client.IppExchangeException.ClientErrorNotFoundException
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.core.IppTag.EventNotification
import de.gmuth.ipp.core.IppTag.Integer
import java.time.Duration
import java.time.Duration.ofSeconds
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

class IppSubscription(
    val printer: IppPrinter,
    private val attributes: IppAttributesGroup
) : IppExchange by printer {

    private val log = getLogger(javaClass.name)

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

    //----------------------------
    // Get-Subscription-Attributes
    //----------------------------

    // RFC 3995 11.2.4.1.2: 'subscription-template', 'subscription-description' or 'all' (default)

    @JvmOverloads
    fun getSubscriptionAttributes(requestedAttributes: List<String>? = null) =
        exchange(ippRequest(GetSubscriptionAttributes, requestedAttributes = requestedAttributes)).subscriptionGroup

    fun updateAttributes() {
        attributes.put(getSubscriptionAttributes())
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

    fun cancel() =
        exchange(ippRequest(CancelSubscription))

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
    // Delegate to IppPrinter
    //-----------------------

    protected fun ippRequest(operation: IppOperation, requestedAttributes: List<String>? = null) =
        printer.ippRequest(operation, requestedAttributes = requestedAttributes)
            .apply { operationGroup.attribute("notify-subscription-id", Integer, id) }

    //------------------------------------
    // Poll and handle event notifications
    //------------------------------------

    var pollHandlesNotifications = false

    val expiresAt: LocalDateTime
        get() = leaseStartedAt.plus(leaseDuration)

    fun expired() = !leaseDuration.isZero && now().isAfter(expiresAt)

    fun pollAndHandleNotifications(
        pollEvery: Duration = ofSeconds(5), // should be larger than 1s
        autoRenewSubscription: Boolean = false,
        handleNotification: (event: IppEventNotification) -> Unit = { log.info { it.toString() } }
    ) {
        fun expiresAfterDelay() = !leaseDuration.isZero && now().plus(pollEvery).isAfter(expiresAt.minusSeconds(2))
        try {
            pollHandlesNotifications = true
            while (pollHandlesNotifications) {
                if (expired()) log.warning { "subscription #$id has expired" }
                getNotifications().forEach { handleNotification(it) }
                if (expiresAfterDelay() && autoRenewSubscription) renew(leaseDuration)
                Thread.sleep(pollEvery.toMillis())
            }
        } catch (clientErrorNotFoundException: ClientErrorNotFoundException) {
            log.info { clientErrorNotFoundException.response!!.statusMessage.toString() }
        }
    }

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder("Subscription #$id").run {
        if (attributes.containsKey("notify-job-id")) append(", job #$jobId")
        if (attributes.containsKey("notify-events")) append(" events=${events.joinToString(",")}")
        if (attributes.containsKey("notify-time-interval")) append(" time-interval=$timeInterval")
        if (attributes.containsKey("notify-lease-duration")) append(" lease-duration=$leaseDuration (expires at $expiresAt)")
        toString()
    }

    @JvmOverloads
    fun log(logger: Logger, level: Level = Level.INFO) =
        attributes.log(logger, level, title = "SUBSCRIPTION #$id")

}