package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// "job-state": type1 enum [RFC8011]
enum class IppJobState(val code: Int, private val registeredValue: String) {

    Pending(3, "pending"),
    PendingHeld(4, "pending-held"),
    Processing(5, "processing"),
    ProcessingStopped(6, "processing-stopped"),
    Canceled(7, "canceled"),
    Aborted(8, "aborted"),
    Completed(9, "completed");

    fun isTerminated() = this in listOf(Canceled, Aborted, Completed)

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-6
    override fun toString() = registeredValue

    companion object {
        fun fromInt(code: Int?) =
                if (code == null) {
                    null
                } else {
                    values().single { it.code == code }
                }
    }

}