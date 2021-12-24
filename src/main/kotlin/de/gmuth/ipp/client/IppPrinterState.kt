package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// "printer-state": type1 enum [RFC8011]
enum class IppPrinterState(val code: Int) {

    Idle(3),
    Processing(4),
    Stopped(5);

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-6
    override fun toString() = name.lowercase()

    companion object {
        fun fromInt(code: Int) = values().single { it.code == code }
    }

}