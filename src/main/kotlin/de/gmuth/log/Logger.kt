package de.gmuth.log

/**
 * Copyright (c) 2022-2023 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*

typealias MessageProducer = () -> Any?

abstract class Logger(val name: String) {

    open var logLevel = OFF
    open fun isEnabled(level: LogLevel) = logLevel != OFF && logLevel <= level
    abstract fun publish(logEvent: LogEvent)

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
    fun log(messageLogLevel: LogLevel, throwable: Throwable? = null, produceMessage: MessageProducer) =
        log(LogEvent(messageLogLevel, produceMessage, throwable))

    fun log(logEvent: LogEvent) = logEvent.run {
        if (isEnabled(logLevel)) publish(logEvent)
    }

    @JvmOverloads
    fun logWithCauseMessages(throwable: Throwable, logLevel: LogLevel = ERROR) {
        throwable.cause?.let { logWithCauseMessages(it, logLevel) }
        log(logLevel) { "${throwable.javaClass.name}: ${throwable.message}" }
    }
}