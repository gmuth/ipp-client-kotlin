package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// "printer-state": type1 enum [RFC8011]
enum class IppPrinterState(val code: Int) {

    Idle(3),
    Processing(4),
    Stopped(5);

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-6
    private fun registeredValue() = name
            .replace("[A-Z]".toRegex()) { "-" + it.value.toLowerCase() }
            .replace("^-".toRegex(), "")

    override fun toString() = registeredValue()

    companion object {
        private val codeMap = values().associateBy(IppPrinterState::code)
        fun fromCode(code: Int): IppPrinterState = codeMap[code]
                ?: throw IppException(String.format("job state code '%02X' undefined", code))

        private val registeredValueMap = values().associateBy(IppPrinterState::registeredValue)
        fun fromRegisteredValue(value: String): IppPrinterState = registeredValueMap[value]
                ?: throw IppException(String.format("job state value '%s' undefined", value))

    }
}