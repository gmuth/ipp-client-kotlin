package de.gmuth.log

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

import java.io.PrintWriter
import java.time.LocalDateTime.now

class ConsoleLogger(name: String) : Logger(name) {

    companion object {
        var defaultLogLevel = Logging.LogLevel.INFO
        // %1=timestamp, %2=loggerName, %3=level, %4=message
        var format: String = "%1\$tT.%1\$tL %2\$-25s %3\$-10s %4\$s%n"
        var simpleClassName = true
        fun configure() {
            Logging.createLogger = ::ConsoleLogger
        }
    }

    init {
        logLevel = defaultLogLevel
    }

    override fun publish(logEvent: LogEvent) {
        val loggerName = if (simpleClassName) name.substringAfterLast(".") else name
        logEvent.run {
            print(format.format(now(), loggerName, logLevel, messageString))
            throwable?.printStackTrace(PrintWriter(System.out, true))
        }
    }
}