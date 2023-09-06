package de.gmuth.log

/**
 * Copyright (c) 2022-2023 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*
import de.gmuth.log.Logging.createLogger

// http://www.slf4j.org
class Slf4jAdapter(name: String) : Logger(name) {

    companion object {
        fun configure() {
            createLogger = ::Slf4jAdapter
        }
    }

    private val slf4jLogger = org.slf4j.LoggerFactory.getLogger(name)

    override fun isEnabled(level: LogLevel) = when (level) {
        OFF -> false
        TRACE -> slf4jLogger.isTraceEnabled
        DEBUG -> slf4jLogger.isDebugEnabled
        INFO -> slf4jLogger.isInfoEnabled
        WARN -> slf4jLogger.isWarnEnabled
        ERROR -> slf4jLogger.isErrorEnabled
    }

    override fun publish(logEvent: LogEvent) = with(logEvent) {
        when (logLevel) {
            OFF -> Unit // don't publish anything
            TRACE -> slf4jLogger.trace(messageString, throwable)
            DEBUG -> slf4jLogger.debug(messageString, throwable)
            INFO -> slf4jLogger.info(messageString, throwable)
            WARN -> slf4jLogger.warn(messageString, throwable)
            ERROR -> slf4jLogger.error(messageString, throwable)
        }
    }
}