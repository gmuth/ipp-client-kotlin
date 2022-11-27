package de.gmuth.log

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.log.Logging.Factory
import de.gmuth.log.Logging.LogLevel.*
import java.io.PrintWriter
import java.time.LocalTime.now
import java.time.format.DateTimeFormatter.ofPattern

typealias MessageProducer = () -> Any?

object Logging {

    var debugLoggingConfig = false
    var defaultLogLevel = INFO
    var consoleWriterEnabled = true
    var consoleWriterFormat: String = "%s%-25s %-5s %s" // timestamp, name, level, message
    var consoleWriterSimpleClassName = true
    var consoleWriterLogTimestamp = true

    enum class LogLevel { OFF, TRACE, DEBUG, INFO, WARN, ERROR }

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
                val timestamp = if (consoleWriterLogTimestamp) now().format(ofPattern("HH:mm:ss.SSS ")) else ""
                println(consoleWriterFormat.format(timestamp, loggerName, messageLogLevel, messageString))
                throwable?.printStackTrace(PrintWriter(System.out, true))
            }
        }

        fun logWithCauseMessages(throwable: Throwable, logLevel: LogLevel = ERROR) {
            throwable.cause?.let { logWithCauseMessages(it, logLevel) }
            log(logLevel) { "${throwable.javaClass.name}: ${throwable.message}" }
        }
    }

    fun interface Factory {
        fun createLogger(name: String): Logger
    }

    var factory = Factory { Logger(it) }
    private val loggerMap: MutableMap<String, Logger> = HashMap()

    @JvmOverloads
    fun getLogger(name: String, logLevel: LogLevel = defaultLogLevel) =
        loggerMap[name] ?: factory.createLogger(name).apply {
            loggerMap[name] = this
            this.logLevel = logLevel
            if (debugLoggingConfig) println("Logging: level=%-5s name=%s".format(logLevel, name))
        }

    fun getLogger(logLevel: LogLevel = defaultLogLevel, noOperation: () -> Unit) =
        getLogger(noOperation.javaClass.enclosingClass.name, logLevel)

}