package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppIntegerTime
import de.gmuth.ipp.core.IppTag
import java.net.URI

data class IppJob(var attributes: IppAttributesGroup) {

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