package de.gmuth.log

/**
 * Copyright (c) 2022-2023 Gerhard Muth
 */

import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

// redirect java util logging messages to de.gmuth.log.Logging
object JulHandler : Handler() {

    override fun publish(logRecord: LogRecord) = logRecord.run {
        Logging.getLogger(loggerName).log(level.toLogLevel(), thrown) { message }
    }

    override fun flush() = Unit
    override fun close() = Unit

    fun addToJulLogger(name: String = "", julLevel: Level = Level.ALL) {
        Logger.getLogger(name).run {
            if (!handlers.contains(JulHandler)) addHandler(JulHandler)
            level = julLevel
        }
    }
}