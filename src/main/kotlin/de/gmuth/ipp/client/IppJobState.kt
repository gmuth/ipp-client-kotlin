package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// "job-state": type1 enum [RFC8011]
//                                                    +----> canceled
//                                                   /
//     +----> pending --------> processing ---------+------> completed
//     |         ^                   ^               \
// --->+         |                   |                +----> aborted
//     |         v                   v               /
//     +----> pending-held    processing-stopped ---+

enum class IppJobState(val code: Int, private val registeredName: String) {

    Pending(3, "pending"),
    PendingHeld(4, "pending-held"),
    Processing(5, "processing"),
    ProcessingStopped(6, "processing-stopped"),
    Canceled(7, "canceled"),
    Aborted(8, "aborted"),
    Completed(9, "completed");

    fun isTerminated() = this in listOf(Canceled, Aborted, Completed)

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-6
    override fun toString() = registeredName

    companion object {
        fun fromInt(code: Int) = values().single { it.code == code }
    }

}