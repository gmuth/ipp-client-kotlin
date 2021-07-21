package de.gmuth.log

import de.gmuth.log.Logging.Factory
import java.io.PrintWriter

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

typealias MessageProducer = () -> Any?

object Logging {

    var debugLoggingConfig = false
    var defaultLogLevel = LogLevel.INFO
    var useSimpleClassName = true
    var consoleWriterEnabled = true
    var consoleWriterFormat: String = "%-25s %-5s %s" // name, level, message

    enum class LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }

    open class Logger(val name: String, var logLevel: LogLevel = defaultLogLevel) {

        @JvmOverloads
        fun trace(throwable: Throwable? = null, messageProducer: MessageProducer) = log(LogLevel.TRACE, throwable, messageProducer)

        @JvmOverloads
        fun debug(throwable: Throwable? = null, messageProducer: MessageProducer) = log(LogLevel.DEBUG, throwable, messageProducer)

        @JvmOverloads
        fun info(throwable: Throwable? = null, messageProducer: MessageProducer) = log(LogLevel.INFO, throwable, messageProducer)

        @JvmOverloads
        fun warn(throwable: Throwable? = null, messageProducer: MessageProducer) = log(LogLevel.WARN, throwable, messageProducer)

        @JvmOverloads
        fun error(throwable: Throwable? = null, messageProducer: MessageProducer) = log(LogLevel.ERROR, throwable, messageProducer)

        @JvmOverloads
        fun log(messageLogLevel: LogLevel, throwable: Throwable? = null, produceMessage: MessageProducer) {
            if (isEnabled(messageLogLevel)) dispatch(messageLogLevel, throwable, produceMessage()?.toString())
        }

        open fun isEnabled(level: LogLevel) = logLevel <= level

        open fun dispatch(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String?) {
            if (consoleWriterEnabled) {
                println(consoleWriterFormat.format(name, messageLogLevel, messageString))
                throwable?.printStackTrace(PrintWriter(System.out, true))
            }
        }
    }

    fun interface Factory {
        fun getLogger(name: String): Logger
    }

    var factory = Factory { Logger(it) }

    @JvmOverloads
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

    fun useSlf4j() {
        useSimpleClassName = false
        factory = Factory { Slf4jLoggerAdapter(it) }
    }

}