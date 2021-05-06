package de.gmuth.log

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*

object Slf4jLogging {

    class Logger(name: String, logLevel: LogLevel = Logging.defaultLogLevel) : Logging.Logger(name, logLevel) {

        private val slf4jLogger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(name)

        override fun isEnabled(level: Logging.LogLevel) =
                with(slf4jLogger) {
                    when (level) { // is enabled
                        TRACE -> isTraceEnabled
                        DEBUG -> isDebugEnabled
                        INFO -> isInfoEnabled
                        WARN -> isWarnEnabled
                        ERROR -> isErrorEnabled
                    }
                }

        override fun dispatch(messageLogLevel: LogLevel, throwable: Throwable?, messageString: String) =
                with(slf4jLogger) {
                    when (messageLogLevel) { // delegate to slf4j
                        TRACE -> trace(messageString, throwable)
                        DEBUG -> debug(messageString, throwable)
                        INFO -> info(messageString, throwable)
                        WARN -> warn(messageString, throwable)
                        ERROR -> error(messageString, throwable)
                    }
                }
    }

    fun configure() {
        Logging.useSimpleClassName = false
        Logging.factory = Logging.Factory { Logger(it) }
    }

}