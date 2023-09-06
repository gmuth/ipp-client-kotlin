package de.gmuth.log

/**
 * Copyright (c) 2022-2023 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*
import de.gmuth.log.Logging.createLogger
import java.util.logging.Level
import java.util.logging.LogManager

// forward log messages to java util logging
// https://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html
class JulAdapter(name: String) : Logger(name) {

    companion object {
        fun configure() {
            createLogger = ::JulAdapter
        }

        fun configureLogManager(configResource: String = "/ippclient-logging.properties") =
            LogManager.getLogManager().readConfiguration(JulAdapter::class.java.getResourceAsStream(configResource))
    }

    private val julLogger = java.util.logging.Logger.getLogger(name)

    override var logLevel: LogLevel
        get() = julLogger.level.toLogLevel()
        set(value) {
            julLogger.level = value.toJulLevel()
        }

    override fun isEnabled(level: LogLevel) =
        julLogger.isLoggable(level.toJulLevel())

    override fun publish(logEvent: LogEvent) = logEvent.run {
        julLogger.log(logLevel.toJulLevel(), messageString, throwable)
    }

    fun LogLevel.toJulLevel(): Level = when (this) {
        OFF -> Level.OFF
        TRACE -> Level.FINER
        DEBUG -> Level.FINE
        INFO -> Level.INFO
        WARN -> Level.WARNING
        ERROR -> Level.SEVERE
    }
}

fun Level.toLogLevel() = when (this) {
    Level.OFF -> OFF
    Level.FINER, Level.FINEST, Level.ALL -> TRACE
    Level.FINE -> DEBUG
    Level.INFO, Level.CONFIG -> INFO
    Level.WARNING -> WARN
    Level.SEVERE -> ERROR
    else -> throw IllegalArgumentException("unknown Level $this")
}