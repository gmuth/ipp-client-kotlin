package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// "job-state": type1 enum [RFC8011]
enum class IppJobState(val code: Int) {

    Pending(3),
    PendingHeld(4),
    Processing(5),
    ProcessingStopped(6),
    Canceled(7),
    Aborted(8),
    Completed(9);

    fun isPending() = this in listOf(Pending, PendingHeld)
    fun isProcessing() = this in listOf(Processing, ProcessingStopped)
    fun isTerminated() = this in listOf(Canceled, Aborted, Completed)

    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-6
    private fun registeredValue() = name
            .replace("[A-Z]".toRegex()) { "-" + it.value.toLowerCase() }
            .replace("^-".toRegex(), "")

    override fun toString() = registeredValue()

    companion object {
        private val codeMap = values().associateBy(IppJobState::code)
        fun fromCode(code: Int): IppJobState = codeMap[code]
                ?: throw IllegalArgumentException(String.format("job state %02X undefined", code))

        private val registeredValueMap = values().associateBy(IppJobState::registeredValue)
        fun fromRegisteredValue(value: String): IppJobState = registeredValueMap[value]
                ?: throw IllegalArgumentException(String.format("job state value '%s' not found", value))

    }
}