package de.gmuth.log

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*
import java.util.logging.Level

class JULLoggerAdapter(name: String) : Logging.Logger(name) {

    val julLogger = java.util.logging.Logger.getLogger(name)

    override fun isEnabled(level: LogLevel) = with(julLogger) {
        when (level) {
            TRACE -> isLoggable(Level.FINER)
            DEBUG -> isLoggable(Level.FINE)
            INFO -> isLoggable(Level.INFO)
            WARN -> isLoggable(Level.WARNING)
            ERROR -> isLoggable(Level.SEVERE)
        }
    }

    override fun publish(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String?) = with(julLogger) {
        when (messageLogLevel) {
            TRACE -> log(Level.FINER, messageString, throwable)
            DEBUG -> log(Level.FINE, messageString, throwable)
            INFO -> log(Level.INFO, messageString, throwable)
            WARN -> log(Level.WARNING, messageString, throwable)
            ERROR -> log(Level.SEVERE, messageString, throwable)
        }
    }

}