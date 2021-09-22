package de.gmuth.log

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*
import java.util.logging.Level

class JULLoggerAdapter(name: String) : Logging.Logger(name) {

    val julLogger = java.util.logging.Logger.getLogger(name)

    private fun toJULLevel(logLevel: LogLevel) = when (logLevel) {
        TRACE -> Level.FINER
        DEBUG -> Level.FINE
        INFO -> Level.INFO
        WARN -> Level.WARNING
        ERROR -> Level.SEVERE
    }

    override fun isEnabled(logLevel: LogLevel) =
            julLogger.isLoggable(toJULLevel(logLevel))

    override fun publish(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String?) =
            julLogger.log(toJULLevel(messageLogLevel), messageString, throwable)

}