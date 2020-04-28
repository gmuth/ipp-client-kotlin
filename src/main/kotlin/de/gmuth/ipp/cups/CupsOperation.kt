package de.gmuth.ipp.cups

import de.gmuth.ipp.core.IppException

/**
 * Copyright (c) 2020 Gerhard Muth
 */

enum class CupsOperation(val code: Short) {

    CupsGetDefault(0x4001),
    CupsGetPrinters(0x4002),
    CupsAddModifyPrinter(0x4003),
    CupsDeletePrinter(0x4004),
    CupsGetClasses(0x4005),
    CuspAddModifyClass(0x4006),
    CupsDeleteClass(0x4007),
    CupsAcceptJobs(0x4008),
    CupsRejectJobs(0x4009),
    CupsSetDefault(0x400A),
    CupsGetDevices(0x400B),
    CupsGetPPDs(0x400C),
    CupsMoveJob(0x400D),
    CupsAuthenticateJob(0x400E),
    CupsGetPPD(0x400F),
    CupsGetDocument(0x4027),
    CupsCreateLocalPrinter(0x4028);

    override fun toString() = name
            .replace("[A-Z]+".toRegex()) { "-" + it.value }
            .replace("^-".toRegex(), "")

    companion object {
        private val codeMap = values().associateBy(CupsOperation::code)
        fun fromShort(code: Short): CupsOperation = codeMap[code]
                ?: throw IppException(String.format("operation code '%04X' unknown", code))
    }

}