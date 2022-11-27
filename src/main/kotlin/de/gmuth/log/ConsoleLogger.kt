package de.gmuth.log

/**
 * Copyright (c) 2022 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import java.io.PrintWriter
import java.time.LocalTime.now
import java.time.format.DateTimeFormatter

class ConsoleLogger(name: String) : Logging.Logger(name, supportLevelConfiguration = true) {

    var format: String = "%s%-25s %-5s %s" // timestamp, name, level, message
    var simpleClassName = true
    var logTimestamp = true

    private var level: LogLevel = Logging.defaultLogLevel

    override var logLevel: LogLevel
        get() = level
        set(value) {
            level = value
        }

    override fun isEnabled(level: LogLevel) = logLevel <= level

    override fun publish(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String?) {
        val loggerName = if (simpleClassName) name.substringAfterLast(".") else name
        val timestamp = if (logTimestamp) now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS ")) else ""
        println(format.format(timestamp, loggerName, messageLogLevel, messageString))
        throwable?.printStackTrace(PrintWriter(System.out, true))
    }

    companion object {
        fun configure() {
            Logging.factory = Logging.Factory { ConsoleLogger(it) }
        }
    }
}