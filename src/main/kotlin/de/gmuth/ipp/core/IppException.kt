package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.log.Logging
import de.gmuth.log.Logging.LogLevel.ERROR

open class IppException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    companion object {
        val log = Logging.getLogger {}
        fun logAllMessages(throwable: Throwable, logLevel: Logging.LogLevel = ERROR) {
            throwable.cause?.let { logAllMessages(it, logLevel) }
            log.log(logLevel) { "${throwable.message}" }
        }
    }

    fun logAllMessages(logLevel: Logging.LogLevel = ERROR) = logAllMessages(this, logLevel)

}