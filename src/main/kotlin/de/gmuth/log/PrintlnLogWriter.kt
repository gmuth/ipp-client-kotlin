package de.gmuth.log

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class PrintlnLogWriter(
        override val category: String = "",
        override var level: Log.Level = Log.Level.WARN

) : Log.Writer {

    var format: String = "%-20s %-5s %s"

    override fun write(messageLevel: Log.Level, message: () -> String) {
        if (messageLevel.ordinal >= level.ordinal) {
            println(String.format(format, category, messageLevel, message()))
        }
    }

    object Factory : Log.Factory {

        private val logWriterMap = mutableMapOf<String, Log.Writer>()

        override fun getWriter(category: String): Log.Writer {
            if (!logWriterMap.containsKey(category)) {
                logWriterMap[category] = PrintlnLogWriter(category)
            }
            return logWriterMap[category]!!
        }
    }

}