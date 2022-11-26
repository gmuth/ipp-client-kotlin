package de.gmuth.log

/**
 * Copyright (c) 2022 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Level.ALL
import java.util.logging.LogRecord
import java.util.logging.Logger

// redirect java util logging message to Logging
object JulHandler : Handler() {

    var defaultFilterLevel: LogLevel = LogLevel.INFO

    // OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
    fun toLogLevel(level: Level) = when (level) {
        Level.FINER, Level.FINEST, ALL -> LogLevel.TRACE
        Level.FINE -> LogLevel.DEBUG
        Level.INFO -> LogLevel.INFO
        Level.WARNING -> LogLevel.WARN
        Level.SEVERE -> LogLevel.ERROR
        else -> LogLevel.INFO
    }

    override fun publish(record: LogRecord) = record.run {
        Logging.getLogger(loggerName, defaultFilterLevel).log(toLogLevel(level), thrown) { message }
    }

    override fun flush() = Unit
    override fun close() = Unit

    fun configure(name: String = "", logLevel: LogLevel = LogLevel.WARN) {
        defaultFilterLevel = logLevel
        Logger.getLogger(name).run {
            if (!handlers.contains(JulHandler)) addHandler(JulHandler)
            level = ALL
        }
    }
}