package de.gmuth.log

/**
 * Copyright (c) 2022 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*
import java.util.logging.Level
import java.util.logging.LogManager

// forward log messages to java util logging
// https://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html
class JulAdapter(name: String) : Logger(name, supportLevelConfiguration = true) {

    private val julLogger = java.util.logging.Logger.getLogger(name)

    override var logLevel: LogLevel
        get() = toLogLevel(julLogger.level)
        set(value) {
            julLogger.level = toJulLevel(value)
        }

    override fun isEnabled(level: LogLevel) =
        julLogger.isLoggable(toJulLevel(level))

    override fun publish(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String?) =
        julLogger.log(toJulLevel(messageLogLevel), messageString, throwable)

    companion object {

        fun toJulLevel(logLevel: LogLevel) = when (logLevel) {
            OFF -> Level.OFF
            TRACE -> Level.FINEST
            DEBUG -> Level.FINE
            INFO -> Level.INFO
            WARN -> Level.WARNING
            ERROR -> Level.SEVERE
        }

        fun toLogLevel(julLevel: Level) = when (julLevel) {
            Level.OFF -> OFF
            Level.FINER, Level.FINEST, Level.ALL -> TRACE
            Level.FINE -> DEBUG
            Level.INFO, Level.CONFIG -> INFO
            Level.WARNING -> WARN
            Level.SEVERE -> ERROR
            else -> throw IllegalArgumentException("unknown Level $julLevel")
        }

        fun configure() {
            Logging.factory = Logging.Factory { JulAdapter(it) }
        }

        fun configureLogManager(configResource: String = "/ipp-client-logging.conf") =
            LogManager.getLogManager().readConfiguration(JulAdapter::class.java.getResourceAsStream(configResource))

    }
}