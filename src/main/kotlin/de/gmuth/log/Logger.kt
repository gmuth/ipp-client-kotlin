package de.gmuth.log

/**
 * Copyright (c) 2022 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*

typealias MessageProducer = () -> Any?

@SuppressWarnings("kotlin:S108", "kotlin:S1186") // base logger that doesn't do anything
open class Logger(val name: String, val supportLevelConfiguration: Boolean = false) {

    open var logLevel: LogLevel
        get() = OFF
        set(value) {}

    open fun isEnabled(level: LogLevel): Boolean = false

    open fun publish(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String?) {}

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

    fun logWithCauseMessages(throwable: Throwable, logLevel: LogLevel = ERROR) {
        throwable.cause?.let { logWithCauseMessages(it, logLevel) }
        log(logLevel) { "${throwable.javaClass.name}: ${throwable.message}" }
    }
}