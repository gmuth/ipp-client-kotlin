package de.gmuth.ipp.core

import java.net.URI

class IppJob {

    var uri: URI? = null
    var id: Int? = null
    var state: IppJobState? = null
    var stateReasons: String? = null

    fun readFrom(jobGroup: IppAttributesGroup) {
        uri = jobGroup["job-uri"]?.value as URI
        id = jobGroup["job-id"]?.value as Int
        state = jobGroup["job-state"]?.value as IppJobState
        stateReasons = jobGroup["job-state-reasons"]?.value as String // 1setOf?
    }

    override fun toString(): String = String.format("IppJob: uri = %s, id = %d, state = %s, stateReasons = %s", uri, id, state, stateReasons)

    companion object {
        fun fromIppAttributesGroup(attributesGroup: IppAttributesGroup) = IppJob().apply { readFrom(attributesGroup) }
    }

}