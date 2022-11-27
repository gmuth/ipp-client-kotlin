package de.gmuth.log

/**
 * Copyright (c) 2022 Gerhard Muth
 */

import de.gmuth.log.JulAdapter.Companion.toLogLevel
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

// redirect java util logging message to Logging
object JulHandler : Handler() {

    override fun publish(record: LogRecord) = with(record) {
        Logging.getLogger(loggerName).log(toLogLevel(level), thrown) { message }
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