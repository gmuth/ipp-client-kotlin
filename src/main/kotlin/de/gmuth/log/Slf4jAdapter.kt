package de.gmuth.log

/**
 * Copyright (c) 2022 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*

// slf4j, http://www.slf4j.org
class Slf4jAdapter(name: String) : Logging.Logger(name, supportLevelConfiguration = false) {

    private val slf4jLogger = org.slf4j.LoggerFactory.getLogger(name)

    override var logLevel: LogLevel
        get() = throw UnsupportedOperationException("get logLevel not supported")
        set(value) {
            throw UnsupportedOperationException("set logLevel not supported")
        }

    override fun isEnabled(level: LogLevel) = with(slf4jLogger) {
        when (level) {
            OFF -> false
            TRACE -> isTraceEnabled
            DEBUG -> isDebugEnabled
            INFO -> isInfoEnabled
            WARN -> isWarnEnabled
            ERROR -> isErrorEnabled
        }
    }

    @SuppressWarnings("kotlin:S108")
    override fun publish(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String?) = with(slf4jLogger) {
        when (messageLogLevel) {
            OFF -> {} // don't publish anything
            TRACE -> trace(messageString, throwable)
            DEBUG -> debug(messageString, throwable)
            INFO -> info(messageString, throwable)
            WARN -> warn(messageString, throwable)
            ERROR -> error(messageString, throwable)
        }
    }

    companion object {
        fun configure() {
            Logging.factory = Logging.Factory { Slf4jAdapter(it) }
        }
    }
}