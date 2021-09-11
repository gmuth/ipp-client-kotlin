package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString
import de.gmuth.log.Logging

class IppEventNotification(
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

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder().run {
        append("subscription #$subscriptionId event #$sequenceNumber:")
        append(" [$subscribedEvent] $text")
        if (attributes.containsKey("notify-job-id")) append(" job #$jobId")
        if (attributes.containsKey("job-state")) append(", job-state=$jobState")
        if (attributes.containsKey("job-state-reasons"))
            append(" (reasons=${jobStateReasons.joinToString(",")})")
        if (attributes.containsKey("printer-state")) append(", printer-state=$printerState")
        if (attributes.containsKey("printer-state-reasons"))
            append(" (reasons=${printerStateReasons.joinToString(",")})")
        toString()
    }

    fun logDetails() =
            attributes.logDetails(title = "event notification #$sequenceNumber $subscribedEvent")

}