package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

import de.gmuth.ipp.attributes.JobState
import de.gmuth.ipp.attributes.PrinterState
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString
import java.net.URI
import java.nio.charset.Charset
import java.time.ZonedDateTime
import java.util.logging.Level
import java.util.logging.Logger

class IppEventNotification(
    val subscription: IppSubscription,
    val attributes: IppAttributesGroup
) {
    val charset: Charset
        get() = attributes.getValue("notify-charset")

    val naturalLanguage: String
        get() = attributes.getValue("notify-natural-language")

    val subscriptionId: Int
        get() = attributes.getValue("notify-subscription-id")

    val sequenceNumber: Int
        get() = attributes.getValue("notify-sequence-number")

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

    val printerUri: URI
        get() = attributes.getValue("notify-printer-uri")

    val printerName: IppString
        get() = attributes.getValue("printer-name")

    val printerState: PrinterState
        get() = PrinterState.fromAttributes(attributes)

    val printerStateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

    val printerIsAcceptingJobs: Boolean
        get() = attributes.getValue("printer-is-accepting-jobs")

    // let a Recipient know when the Event Notification occurred (RFC 3996 5.2.2)
    val printerUpTime: ZonedDateTime
        get() = attributes.getZonedDateTimeValue("printer-up-time")

    // Get job of event origin
    fun getJob() = subscription.printer.getJob(jobId)

    // Get printer of event origin
    fun getPrinter(getPrinterAttributesOnInit: Boolean = false) = IppPrinter(
        printerUri,
        ippClient = subscription.printer.ippClient,
        getPrinterAttributesOnInit = getPrinterAttributesOnInit
    )

    @SuppressWarnings("kotlin:S3776")
    override fun toString() = StringBuilder().run {
        append(printerUpTime.toLocalDateTime())
        append(" EventNotification #$sequenceNumber")
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

    @JvmOverloads
    fun log(logger: Logger, level: Level = Level.INFO) =
        attributes.log(logger, level, title = "EVENT_NOTIFICATION #$sequenceNumber [$subscribedEvent] $text")

}