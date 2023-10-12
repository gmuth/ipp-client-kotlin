package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

import de.gmuth.ipp.attributes.JobState
import de.gmuth.ipp.attributes.PrinterState
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString

class IppEventNotification(
    private val subscription: IppSubscription,
    eventNotificationAttributes: IppAttributesGroup
) : IppObject(subscription, eventNotificationAttributes) {

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

    val jobState: JobState
        get() = JobState.fromAttributes(attributes)

    val jobStateReasons: List<String>
        get() = attributes.getValues("job-state-reasons")

    val jobImpressionsCompleted: Int
        get() = attributes.getValue("job-impressions-completed")

    val printerName: IppString
        get() = attributes.getValue("printer-name")

    val printerState: PrinterState
        get() = PrinterState.fromAttributes(attributes)

    val printerStateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

    val printerIsAcceptingJobs: Boolean
        get() = attributes.getValue("printer-is-accepting-jobs")

    // Get job from printer
    fun getJob() = subscription.printer.getJob(jobId)

    override fun objectName() = "Event notification #$sequenceNumber $subscribedEvent"

    @SuppressWarnings("kotlin:S3776")
    override fun toString() = StringBuilder().run {
        append("Event #$sequenceNumber:")
        append(" [$subscribedEvent] $text")
        with(attributes) {
            if (containsKey("notify-job-id")) append(", job #$jobId")
            if (containsKey("job-state")) append(", job-state=$jobState")
            if (containsKey("job-state-reasons")) append(" (reasons=${jobStateReasons.joinToString(",")})")
            if (containsKey("printer-name")) append(", printer-name=$printerName")
            if (containsKey("printer-state")) append(", printer-state=$printerState")
            if (containsKey("printer-state-reasons")) append(" (reasons=${printerStateReasons.joinToString(",")})")
        }
        toString()
    }
}