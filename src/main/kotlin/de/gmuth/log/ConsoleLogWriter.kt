package de.gmuth.log

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class ConsoleLogWriter(
        override val category: String = "",
        override var level: Log.Level = Log.Level.WARN,
        var format: String = "%-20s %-5s %s"

) : Log.Writer {

    object Factory : Log.Factory {
        override fun getWriter(category: String) = ConsoleLogWriter(category)
    }

    override fun write(messageLevel: Log.Level, message: () -> String) {
        println(String.format(format, category, messageLevel, message()))
    }

}