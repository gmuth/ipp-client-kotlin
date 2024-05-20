package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2024 Gerhard Muth
 */

import de.gmuth.ipp.client.IppOperationException.ClientErrorNotFoundException
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

@SuppressWarnings("kotlin:S1192")
class IppSubscription(
    val printer: IppPrinter,
    val attributes: IppAttributesGroup,
    startLease: Boolean = true
) : IppExchange by printer {

    private val logger = getLogger(javaClass.name)
    private var lastSequenceNumber: Int = 0
    private var leaseStartedAt: LocalDateTime? = if (startLease) now() else null

    init {
        // Create-Subscription only responds with id
        if (attributes.size == 1 && attributes.containsKey("notify-subscription-id")) updateAttributes()
    }

    val id: Int
        get() = attributes.getValue("notify-subscription-id")

    val leaseDuration: Duration
        get() = attributes.getDurationOfSecondsValue("notify-lease-duration")

    val events: List<String>
        get() = attributes.getValues("notify-events")

    val jobId: Int
        get() = attributes.getValue("notify-job-id")

    val subscriberUserName: IppString
        get() = attributes.getValue("notify-subscriber-user-name")

    val timeInterval: Duration
        get() = attributes.getDurationOfSecondsValue("notify-time-interval")

    private fun expires() = when {
        leaseDuration.isZero -> "(never expires)"
        leaseStartedAt == null -> ""
        else -> "(expires $expiresAt)"
    }

    //----------------------------
    // Get-Subscription-Attributes
    //----------------------------

    // RFC 3995 11.2.4.1.2: 'subscription-template', 'subscription-description' or 'all' (default)
    // BUG: CUPS ignores unsupported requested attributes, e.g. notify-lease-expiration-time
    @JvmOverloads
    fun getSubscriptionAttributes(requestedAttributes: Collection<String>? = null) = exchange(
        subscriptionRequest(GetSubscriptionAttributes, requestedAttributes = requestedAttributes)
    )

    fun updateAttributes() {
        attributes.put(getSubscriptionAttributes().subscriptionGroup)
    }

    //------------------
    // Get-Notifications
    //------------------

    @JvmOverloads
    fun getNotifications(notifySequenceNumber: Int? = lastSequenceNumber + 1): List<IppEventNotification> {
        val request = subscriptionRequest(GetNotifications).apply {
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

    fun cancel() = exchange(subscriptionRequest(CancelSubscription))
        .also { logger.info { "Canceled $this" } }

    //-------------------
    // Renew-Subscription
    //-------------------

    @JvmOverloads
    fun renew(leaseDuration: Duration? = null) = exchange(
        subscriptionRequest(RenewSubscription).apply {
            createSubscriptionAttributesGroup(notifyLeaseDuration = leaseDuration)
        }
    ).also {
        leaseStartedAt = now()
        updateAttributes()
        logger.fine { "Renewed $this" }
    }

    //-----------------------
    // Delegate to IppPrinter
    //-----------------------

    private fun subscriptionRequest(operation: IppOperation, requestedAttributes: Collection<String>? = null) =
        printer.ippRequest(operation, requestedAttributes = requestedAttributes)
            .apply { operationGroup.attribute("notify-subscription-id", Integer, id) }

    //------------------------------------
    // Poll and handle event notifications
    //------------------------------------

    var pollHandlesNotifications = false

    val expiresAt: LocalDateTime? // null = never expires
        get() = if (leaseDuration.isZero) null else {
            require(leaseStartedAt != null) { "leaseStartedAt required to calculate expiration" }
            leaseStartedAt!!.plus(leaseDuration)
        }

    fun expired() = expiresAt != null && now().isAfter(expiresAt)
    fun expiryAvailable() = leaseStartedAt != null && !leaseDuration.isZero

    @JvmOverloads
    fun pollAndHandleNotifications(
        pollEvery: Duration = ofSeconds(5), // should be larger than 1s
        autoRenewSubscription: Boolean = false,
        handleNotification: (event: IppEventNotification) -> Unit = { logger.info { it.toString() } }
    ) {
        fun expiresAfterDelay() = expiresAt != null && now().plus(pollEvery).isAfter(expiresAt!!.minusSeconds(2))
        try {
            pollHandlesNotifications = true
            while (pollHandlesNotifications) {
                if (expiryAvailable() && expired()) logger.warning { "Subscription #$id has expired" }
                getNotifications().forEach { handleNotification(it) }
                if (expiryAvailable() && expiresAfterDelay() && autoRenewSubscription) renew(leaseDuration)
                Thread.sleep(pollEvery.toMillis())
            }
        } catch (clientErrorNotFoundException: ClientErrorNotFoundException) {
            logger.info { clientErrorNotFoundException.response!!.statusMessage.toString() }
        }
    }

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder("Subscription #$id").run {
        if (attributes.containsKey("notify-job-id")) append(", job #$jobId")
        if (attributes.containsKey("notify-events")) append(" events=${events.joinToString(",")}")
        if (attributes.containsKey("notify-time-interval")) append(" time-interval=$timeInterval")
        if (attributes.containsKey("notify-lease-duration")) append(" lease-duration=$leaseDuration ${expires()}")
        toString()
    }

    @JvmOverloads
    fun log(logger: Logger, level: Level = Level.INFO) = attributes.log(
        logger, level,
        title = "SUBSCRIPTION #$id ${if (attributes.containsKey("notify-lease-duration")) expires() else ""}"
    )
}