package de.gmuth.ipp.core

import java.net.URI
import java.time.LocalDateTime

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppJob(jobGroup: IppAttributesGroup) {

    lateinit var uri: URI
    var id: Int = 0
    var state: IppJobState? = null
    var stateReasons: List<String>? = null

    var printerUri: URI? = null
    var name: String? = null
    var originatingUserName: String? = null

    var timeAtCreation: LocalDateTime? = null
    var timeAtProcessing: LocalDateTime? = null
    var timeAtCompleted: LocalDateTime? = null
    var printerUpTime: LocalDateTime? = null

    var impressions: Int? = null
    var impressionsCompleted: Int? = null
    var mediaSheets: Int? = null
    var mediaSheetsCompleted: Int? = null

    init {
        readFrom(jobGroup)
    }

    @Suppress("UNCHECKED_CAST")
    fun readFrom(jobGroup: IppAttributesGroup) = with(jobGroup) {
        uri = get("job-uri")?.value as URI
        id = get("job-id")?.value as Int
        state = get("job-state")?.value as IppJobState?
        stateReasons = get("job-state-reasons")?.values as List<String>?

        printerUri = get("job-printer-uri")?.value as URI?
        name = get("job-name")?.value as String?
        originatingUserName = get("job-originating-user-name")?.value as String?

        fun getTimeAt(name: String) = IppTime.toLocalDateTime(get(name)?.value as Int?)
        timeAtCreation = getTimeAt("time-at-creation")
        timeAtProcessing = getTimeAt("time-at-processing")
        timeAtCompleted = getTimeAt("time-at-completed")
        printerUpTime = getTimeAt("job-printer-up-time")

        impressions = get("job-impressions")?.value as Int?
        impressionsCompleted = get("job-impressions-completed")?.value as Int?
        mediaSheets = get("job-media-sheets")?.value as Int?
        mediaSheetsCompleted = get("job-media-sheets-completed")?.value as Int?
    }

    override fun toString(): String {
        val stateString =
                if (state == null) ""
                else ", state = $state, stateReasons = $stateReasons"

        return "IppJob: uri = $uri, id = $id$stateString"
    }

    fun logDetails() {
        println("JOB")
        println("  uri = $uri")
        println("  id = $id")
        logAttributeIfValueNotNull("state", state)
        logAttributeIfValueNotNull("stateReasons", stateReasons)
        logAttributeIfValueNotNull("printerUri", printerUri)
        logAttributeIfValueNotNull("name", name)
        logAttributeIfValueNotNull("originatingUserName", originatingUserName)
        logAttributeIfValueNotNull("timeAtCreation", timeAtCreation)
        logAttributeIfValueNotNull("timeAtProcessing", timeAtProcessing)
        logAttributeIfValueNotNull("timeAtCompleted", timeAtCompleted)
        logAttributeIfValueNotNull("printerUpTime", printerUpTime)
        logAttributeIfValueNotNull("impressions", impressions)
        logAttributeIfValueNotNull("impressionsCompleted", impressionsCompleted)
        logAttributeIfValueNotNull("mediaSheets", mediaSheets)
        logAttributeIfValueNotNull("mediaSheetsCompleted", mediaSheetsCompleted)
    }

    private fun logAttributeIfValueNotNull(name: String, value: Any?) {
        if (value != null) println("  $name = $value")
    }

}

fun IppAttributesGroup.toIppJob() =
        if (tag == IppTag.Job) IppJob(this)
        else throw IppException("not a job group: $tag")