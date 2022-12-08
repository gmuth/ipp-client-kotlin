package de.gmuth.log

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.log.Logging.Factory
import de.gmuth.log.Logging.LogLevel.*

object Logging {

    var defaultLogLevel = INFO

    enum class LogLevel { OFF, TRACE, DEBUG, INFO, WARN, ERROR }

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