package de.gmuth.log

import de.gmuth.log.Logging.Factory
import de.gmuth.log.Logging.LogLevel.*
import java.io.PrintWriter

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

typealias MessageProducer = () -> Any?

object Logging {

    var debugLoggingConfig = false
    var defaultLogLevel = INFO
    var consoleWriterEnabled = true
    var consoleWriterFormat: String = "%-25s %-5s %s" // name, level, message
    var consoleWriterSimpleClassName = true

    enum class LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }

    open class Logger(val name: String, var logLevel: LogLevel = defaultLogLevel) {

        @JvmOverloads
        fun trace(throwable: Throwable? = null, messageProducer: MessageProducer = { "" }) =
                log(TRACE, throwable, messageProducer)

        @JvmOverloads
        fun debug(throwable: Throwable? = null, messageProducer: MessageProducer = { "" }) =
                log(DEBUG, throwable, messageProducer)

        @JvmOverloads
        fun info(throwable: Throwable? = null, messageProducer: MessageProducer = { "" }) =
                log(INFO, throwable, messageProducer)

        @JvmOverloads
        fun warn(throwable: Throwable? = null, messageProducer: MessageProducer = { "" }) =
                log(WARN, throwable, messageProducer)

        @JvmOverloads
        fun error(throwable: Throwable? = null, messageProducer: MessageProducer = { "" }) =
                log(ERROR, throwable, messageProducer)

        @JvmOverloads
        fun log(messageLogLevel: LogLevel, throwable: Throwable? = null, produceMessage: MessageProducer) {
            if (isEnabled(messageLogLevel)) publish(messageLogLevel, throwable, produceMessage()?.toString())
        }

        open fun isEnabled(level: LogLevel) = logLevel <= level

        open fun publish(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String?) {
            if (consoleWriterEnabled) {
                val loggerName = if (consoleWriterSimpleClassName) name.substringAfterLast(".") else name
                println(consoleWriterFormat.format(loggerName, messageLogLevel, messageString))
                throwable?.printStackTrace(PrintWriter(System.out, true))
            }
        }
    }

    fun interface Factory {
        fun getLogger(name: String): Logger
    }

    var factory = Factory { Logger(it) }

    @JvmOverloads
    fun getLogger(name: String, logLevel: LogLevel = defaultLogLevel) = factory.getLogger(name).apply {
        if (debugLoggingConfig) println("new Logger: level=%-5s name=%s".format(logLevel, name))
        this.logLevel = logLevel
    }

    fun getLogger(logLevel: LogLevel = defaultLogLevel, noOperation: () -> Unit) =
            getLogger(noOperation.javaClass.enclosingClass.name, logLevel)

    fun useSlf4j() {
        factory = Factory { Slf4jLoggerAdapter(it) }
    }

}