package de.gmuth.log

/**
 * Copyright (c) 2022 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*
import java.util.logging.Level
import java.util.logging.LogManager

// java util logging, https://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html
// forward log messages to java util logging appender
class JulAdapter(name: String) : Logging.Logger(name) {

    private val julLogger = java.util.logging.Logger.getLogger(name)

    private fun toJulLevel(logLevel: LogLevel) = when (logLevel) {
        OFF -> Level.OFF
        TRACE -> Level.FINER
        DEBUG -> Level.FINE
        INFO -> Level.INFO
        WARN -> Level.WARNING
        ERROR -> Level.SEVERE
    }

    override fun isEnabled(level: LogLevel) =
        julLogger.isLoggable(toJulLevel(level))

    override fun publish(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String?) =
        julLogger.log(toJulLevel(messageLogLevel), messageString, throwable)

    companion object {
        fun configure(configResource: String? = null) { // e.g. "/ipp-client-logging.conf"
            configResource?.let {
                LogManager.getLogManager().readConfiguration(JulAdapter::class.java.getResourceAsStream(it))
            }
            Logging.factory = Logging.Factory { JulAdapter(it) }
        }
    }
}