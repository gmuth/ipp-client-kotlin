package de.gmuth.log

import de.gmuth.log.Logging.Factory
import java.io.PrintWriter

/**
 * Copyright (c) 2020 Gerhard Muth
 */

object Logging {

    var debugLoggingConfig = false
    var defaultLogLevel = LogLevel.INFO
    var useSimpleClassName = true
    var consoleWriterEnabled = true
    var consoleWriterFormat: String = "%-25s %-5s %s"

    enum class LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }

    open class Logger(val name: String, var logLevel: LogLevel = defaultLogLevel) {

        fun trace(messageProducer: () -> Any?) = trace(null, messageProducer)
        fun debug(messageProducer: () -> Any?) = debug(null, messageProducer)
        fun info(messageProducer: () -> Any?) = info(null, messageProducer)
        fun warn(messageProducer: () -> Any?) = warn(null, messageProducer)
        fun error(messageProducer: () -> Any?) = error(null, messageProducer)

        fun trace(throwable: Throwable?, messageProducer: () -> Any?) = log(LogLevel.TRACE, throwable, messageProducer)
        fun debug(throwable: Throwable?, messageProducer: () -> Any?) = log(LogLevel.DEBUG, throwable, messageProducer)
        fun info(throwable: Throwable?, messageProducer: () -> Any?) = log(LogLevel.INFO, throwable, messageProducer)
        fun warn(throwable: Throwable?, messageProducer: () -> Any?) = log(LogLevel.WARN, throwable, messageProducer)
        fun error(throwable: Throwable?, messageProducer: () -> Any?) = log(LogLevel.ERROR, throwable, messageProducer)

        fun log(messageLogLevel: LogLevel, throwable: Throwable?, messageProducer: () -> Any?) {
            if (isEnabled(messageLogLevel)) dispatch(messageLogLevel, throwable, messageProducer()?.toString() ?: "null")
        }

        fun log(messageLogLevel: LogLevel, messageProducer: () -> Any?) =
                log(messageLogLevel, null, messageProducer)

        open fun isEnabled(level: LogLevel) = logLevel <= level

        open fun dispatch(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String) {
            if (consoleWriterEnabled) {
                println(String.format(consoleWriterFormat, name, messageLogLevel, messageString))
                throwable?.printStackTrace(PrintWriter(System.out, true))
            }
        }
    }

    fun interface Factory {
        fun getLogger(name: String): Logger
    }

    var factory = Factory { Logger(it) }

    fun getLogger(name: String, logLevel: LogLevel = defaultLogLevel): Logger {
        if (debugLoggingConfig) println(String.format("new Logger: level=%-5s name=%s", logLevel, name))
        return factory.getLogger(name).apply { this.logLevel = logLevel }
    }

    fun getLogger(logLevel: LogLevel = defaultLogLevel, noOperation: () -> Unit): Logger {
        val loggerName = with(noOperation.javaClass.enclosingClass.name) {
            if (useSimpleClassName) substringAfterLast(".") else this
        }
        return getLogger(loggerName, logLevel)
    }

}