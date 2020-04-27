package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppIntegerTime
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppTag
import java.net.URI
import java.time.Duration

class IppJob(
        val printer: IppPrinter,
        var attributes: IppAttributesGroup
) {
    //--------------
    // IppAttributes
    //--------------

    val id: Int
        get() = attributes.getValue("job-id")

    val uri: URI
        get() = attributes.getValue("job-uri")

    val state: IppJobState?
        get() = IppJobState.fromInt(attributes.getValue("job-state") as Int?)

    val stateReasons: List<String>?
        get() = attributes.getValues("job-state-reasons")

    val impressionsCompleted: Int?
        get() = attributes.getValue("job-impressions-completed")

    fun isTerminated() = state != null && state!!.isTerminated()

    //-------------------
    // Get-Job-Attributes
    //-------------------

    private fun getJobAttributes(): IppAttributesGroup {
        val response = exchangeSuccessfulIppJobRequest(IppOperation.GetJobAttributes, id)
        return response.jobGroup
    }

    fun updateAttributes() {
        attributes = getJobAttributes()
    }

    //------------------------------------------
    // Wait for terminal state (RFC 8011 5.3.7.)
    //------------------------------------------

    fun waitForTermination(refreshRate: Duration = Duration.ofSeconds(1)) {
        println("wait for terminal state of job #$id")
        do {
            Thread.sleep(refreshRate.toMillis())
            attributes = getJobAttributes()
            println("job-state = $state, job-impressions-completed = $impressionsCompleted")
        } while (!isTerminated())
    }

    //-----------
    // Cancel-Job
    //-----------

    fun cancel() {
        exchangeSuccessfulIppJobRequest(IppOperation.CancelJob, id)
    }

    //---------
    // Hold-Job
    //---------

    fun hold() {
        exchangeSuccessfulIppJobRequest(IppOperation.HoldJob, id)
    }

    //------------
    // Release-Job
    //-------------

    fun release() {
        exchangeSuccessfulIppJobRequest(IppOperation.ReleaseJob, id)
    }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    private fun exchangeSuccessfulIppJobRequest(operation: IppOperation, jobId: Int) =
            printer.exchangeSuccessfulIppJobRequest(operation, jobId)

    // -------
    // Logging
    // -------

    override fun toString(): String {
        val stateString =
                if (state == null) {
                    ""
                } else {
                    ", state = $state, stateReasons = ${stateReasons?.joinToString(",")}"
                }
        return "IppJob: id = $id, uri = $uri$stateString"
    }

    fun logDetails() {
        println("JOB-$id")
        for (attribute in attributes.values) {
            with(attribute) {
                if (name.startsWith("time-at") && tag == IppTag.Integer) {
                    val integerTime = IppIntegerTime.fromInt(attributes.getValue(name) as Int?)
                    println("  $name ($tag) = $integerTime")
                } else {
                    println("  $this")
                }
            }
        }
    }

}