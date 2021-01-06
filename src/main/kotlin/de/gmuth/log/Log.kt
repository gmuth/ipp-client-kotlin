package de.gmuth.log

/**
 * Copyright (c) 2020 Gerhard Muth
 */

object Log {

    enum class Level {
        TRACE, DEBUG, INFO, WARN, ERROR;

        fun includes(level: Level) = ordinal <= level.ordinal
    }

    interface Writer {
        val category: String
        var level: Level

        fun trace(message: () -> String) = log(Level.TRACE, message)
        fun debug(message: () -> String) = log(Level.DEBUG, message)
        fun info(message: () -> String) = log(Level.INFO, message)
        fun warn(message: () -> String) = log(Level.WARN, message)
        fun error(message: () -> String) = log(Level.ERROR, message)

        fun log(messageLevel: Level, message: () -> String) {
            if (level.includes(messageLevel)) write(messageLevel, message)
        }

        // implement this method in your own LogWriter
        fun write(messageLevel: Level, message: () -> String)
    }

    interface Factory {
        fun getWriter(category: String): Writer
    }

    // default log factory for simple console logging
    var factory: Factory = ConsoleLogWriter.Factory

    var defaultLevel = Level.WARN

    fun getWriter(category: String, level: Level = defaultLevel) =
            factory.getWriter(category).apply { this.level = level }

}