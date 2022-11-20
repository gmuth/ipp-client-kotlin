package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2022 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString
import de.gmuth.log.Logging

class IppEventNotification(
    val subscription: IppSubscription,
    val attributes: IppAttributesGroup
) {
    companion object {
        val log = Logging.getLogger {}
    }

    val sequenceNumber: Int
        get() = attributes.getValue("notify-sequence-number")

    val subscriptionId: Int
        get() = attributes.getValue("notify-subscription-id")

    val subscribedEvent: String
        get() = attributes.getValue("notify-subscribed-event")

    val text: IppString
        get() = attributes.getValue("notify-text")

    val jobId: Int
        get() = attributes.getValue("notify-job-id")

    val jobState: IppJobState
        get() = IppJobState.fromInt(attributes.getValue("job-state"))

    val jobStateReasons: List<String>
        get() = attributes.getValues("job-state-reasons")

    val jobImpressionsCompleted: Int
        get() = attributes.getValue("job-impressions-completed")

    val printerState: IppPrinterState
        get() = IppPrinterState.fromInt(attributes.getValue("printer-state"))

    val printerStateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

    // get job from printer
    fun getJob() = subscription.printer.getJob(jobId)

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder().run {
        append("subscription #$subscriptionId event #$sequenceNumber:")
        append(" [$subscribedEvent] $text")
        with(attributes) {
            if (contains("notify-job-id")) append(", job #$jobId")
            if (contains("job-state")) append(", job-state=$jobState")
            if (contains("job-state-reasons")) append(" (reasons=${jobStateReasons.joinToString(",")})")
            if (contains("printer-state")) append(", printer-state=$printerState")
            if (contains("printer-state-reasons")) append(" (reasons=${printerStateReasons.joinToString(",")})")
        }
        toString()
    }

    fun logDetails() =
        attributes.logDetails(title = "event notification #$sequenceNumber $subscribedEvent")

}