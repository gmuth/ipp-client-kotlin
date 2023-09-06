package de.gmuth.log

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

object Logging {

    enum class LogLevel { OFF, TRACE, DEBUG, INFO, WARN, ERROR }

    var createLogger: ((name: String) -> Logger)? = ::ConsoleLogger // default logger

    private val loggerMap: MutableMap<String, Logger> = mutableMapOf()

    fun disable() {
        createLogger = null
    }

    fun getLogger(name: String) =
        if (createLogger == null)
            NoOperationLogger
        else
            loggerMap[name] ?: createLogger!!(name).apply {
                loggerMap[name] = this
            }

    fun getLogger(noOperation: () -> Unit) =
        getLogger(noOperation.javaClass.enclosingClass.name)

    object NoOperationLogger : Logger("") {
        override fun isEnabled(level: LogLevel) = false
        override fun publish(logEvent: LogEvent) = Unit
    }
}