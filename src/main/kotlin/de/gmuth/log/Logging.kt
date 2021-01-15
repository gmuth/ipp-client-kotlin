package de.gmuth.log

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

        fun trace(messageProducer: () -> Any?) = log(LogLevel.TRACE, messageProducer)
        fun debug(messageProducer: () -> Any?) = log(LogLevel.DEBUG, messageProducer)
        fun info(messageProducer: () -> Any?) = log(LogLevel.INFO, messageProducer)
        fun warn(messageProducer: () -> Any?) = log(LogLevel.WARN, messageProducer)
        fun error(messageProducer: () -> Any?) = log(LogLevel.ERROR, messageProducer)

        fun log(messageLogLevel: LogLevel, messageProducer: () -> Any?) {
            if (isEnabled(messageLogLevel)) dispatch(messageLogLevel, messageProducer()?.toString() ?: "null")
        }

        open fun isEnabled(level: LogLevel): Boolean {
            return logLevel <= level
        }

        open fun dispatch(messageLogLevel: LogLevel, messageString: String) {
            if (consoleWriterEnabled) writeConsole(messageLogLevel, messageString)
        }

        open fun writeConsole(messageLogLevel: LogLevel, message: String) {
            println(String.format(consoleWriterFormat, name, messageLogLevel, message))
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
        val fullClassName = className(noOperation)
        val loggerName = if (useSimpleClassName) fullClassName.substringAfterLast(".") else fullClassName
        return getLogger(loggerName, logLevel)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun className(noinline noOperation: () -> Unit) =
            with(noOperation.javaClass.name) {
                when {
                    contains("Kt$") -> substringBefore("Kt$")
                    contains("$") -> substringBefore("$")
                    else -> this
                }
            }
}