package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// "printer-state": type1 enum [RFC8011]
enum class IppPrinterState(val code: Int, private val registeredValue: String) {

    Idle(3, "idle"),
    Processing(4, "processing"),
    Stopped(5, "stopped");

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-6
    override fun toString() = registeredValue

    companion object {
        fun fromInt(code: Int) = values().single { it.code == code }
    }

}