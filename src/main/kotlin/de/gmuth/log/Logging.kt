package de.gmuth.log

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.log.Logging.Factory
import de.gmuth.log.Logging.LogLevel.*

typealias MessageProducer = () -> Any?

object Logging {

    var defaultLogLevel = INFO

    enum class LogLevel { OFF, TRACE, DEBUG, INFO, WARN, ERROR }

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

    fun interface Factory {
        fun createLogger(name: String): Logger
    }

    var factory = Factory { ConsoleLogger(it) }
    private val loggerMap: MutableMap<String, Logger> = HashMap()

    fun getLogger(name: String) = loggerMap[name] ?: factory.createLogger(name).apply {
        loggerMap[name] = this
        if (supportLevelConfiguration) logLevel = defaultLogLevel
    }

    fun getLogger(level: LogLevel? = null, noOperation: () -> Unit) =
        getLogger(noOperation.javaClass.enclosingClass.name).apply {
            level?.let { logLevel = level }
        }

    fun configureLevel(name: String, level: LogLevel, throwIfNotSupported: Boolean = true) = getLogger(name).run {
        if (supportLevelConfiguration) logLevel = level
        else if (throwIfNotSupported)
            throw UnsupportedOperationException("Logger implementation does not support level configuration.")
    }

    fun factorySimpleClassNameStartsWith(name: String) = factory.javaClass.simpleName.startsWith(name)

    fun disable() {
        factory = Factory { Logger(it) }
    }
}