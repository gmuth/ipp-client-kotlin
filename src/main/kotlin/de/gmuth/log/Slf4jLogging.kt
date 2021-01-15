package de.gmuth.log

import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*

class Slf4JLogging {

    class Logger(name: String, logLevel: LogLevel = Logging.defaultLogLevel) : Logging.Logger(name, logLevel) {

        private val slf4jLogger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(name)

        override fun isEnabled(level: Logging.LogLevel) =
                with(slf4jLogger) {
                    when (level) {
                        TRACE -> isTraceEnabled
                        DEBUG -> isDebugEnabled
                        INFO -> isInfoEnabled
                        WARN -> isWarnEnabled
                        ERROR -> isErrorEnabled
                    }
                }

        override fun dispatch(messageLogLevel: LogLevel, messageString: String) =
                with(slf4jLogger) {
                    when (messageLogLevel) {
                        TRACE -> trace(messageString)
                        DEBUG -> debug(messageString)
                        INFO -> info(messageString)
                        WARN -> warn(messageString)
                        ERROR -> error(messageString)
                    }
                }
    }

    companion object {
        fun configure() {
            Logging.useSimpleClassName = false
            Logging.factory = Logging.Factory { Logger(it) }
        }
    }

}