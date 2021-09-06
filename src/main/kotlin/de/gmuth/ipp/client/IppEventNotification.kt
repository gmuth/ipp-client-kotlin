package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString
import de.gmuth.log.Logging

/**
 * Copyright (c) 2021 Gerhard Muth
 */

class IppEventNotification(
        var attributes: IppAttributesGroup
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

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder().apply {
        append("event #$sequenceNumber subscription #$subscriptionId [$subscribedEvent] $text")
        if (attributes.containsKey("notify-job-id")) append(" job #$jobId")
        if (attributes.containsKey("job-state")) append(" job-state=$jobState")
        if (attributes.containsKey("job-state-reasons"))
            append(" job-state-reasons=${jobStateReasons.joinToString(",")}")
    }.toString()

    fun logDetails() {
        attributes.logDetails(title = "event notification #$sequenceNumber $subscribedEvent")
    }
}