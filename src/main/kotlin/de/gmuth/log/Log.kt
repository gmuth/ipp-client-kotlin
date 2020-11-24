package de.gmuth.log

/**
 * Copyright (c) 2020 Gerhard Muth
 */

object Log {

    enum class Level { TRACE, DEBUG, INFO, WARN, ERROR }

    interface Writer {
        val category: String
        var level: Level

        fun write(messageLevel: Level, message: () -> String)

        fun trace(message: () -> String) = write(Level.TRACE, message)

        fun debug(message: () -> String) = write(Level.DEBUG, message)

        fun info(message: () -> String) = write(Level.INFO, message)

        fun warn(message: () -> String) = write(Level.WARN, message)

        fun error(message: () -> String) = write(Level.ERROR, message)
    }

    interface Factory {
        fun getWriter(category: String): Writer
    }

    var factory: Factory = PrintlnLogWriter.Factory

    fun getWriter(category: String, level: Level? = null): Writer {
        val writer = factory.getWriter(category)
        if (level != null) writer.level = level
        return writer
    }

}