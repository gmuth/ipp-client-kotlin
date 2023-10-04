package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag

// "job-state": type1 enum [RFC8011]
//                                                    +----> canceled
//                                                   /
//     +----> pending --------> processing ---------+------> completed
//     |         ^                   ^               \
// --->+         |                   |                +----> aborted
//     |         v                   v               /
//     +----> pending-held    processing-stopped ---+

enum class JobState(val code: Int, private val registeredName: String) : IppAttributeBuilder {

    Pending(3, "pending"),
    PendingHeld(4, "pending-held"),
    Processing(5, "processing"),
    ProcessingStopped(6, "processing-stopped"),
    Canceled(7, "canceled"),
    Aborted(8, "aborted"),
    Completed(9, "completed");

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-6
    override fun toString() = registeredName

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("job-state", IppTag.Enum, code)

    companion object {
        private fun fromInt(code: Int) = values().single { it.code == code }
        fun fromAttributes(attributes: IppAttributesGroup) = fromInt(attributes.getValue("job-state"))
    }

}