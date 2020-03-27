package de.gmuth.ipp.core

import java.net.URI

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppJob {

    var uri: URI? = null
    var id: Int? = null
    var state: IppJobState? = null
    var stateReasons: List<String>? = null

    fun readFrom(jobGroup: IppAttributesGroup) = with(jobGroup) {
        uri = get("job-uri")?.value as URI
        id = get("job-id")?.value as Int
        state = get("job-state")?.value as IppJobState
        stateReasons = get("job-state-reasons")?.values as List<String>
    }

    override fun toString(): String = String.format("IppJob: uri = %s, id = %d, state = %s, stateReasons = %s", uri, id, state, stateReasons)

    companion object {
        fun fromIppAttributesGroup(attributesGroup: IppAttributesGroup) = IppJob().apply { readFrom(attributesGroup) }
    }

}