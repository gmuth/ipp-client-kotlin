package de.gmuth.log

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import android.util.Log
import de.gmuth.log.Logging.LogLevel
import de.gmuth.log.Logging.LogLevel.*
import de.gmuth.log.Logging.createLogger

// https://developer.android.com/reference/android/util/Log
class AndroidLogAdapter(name: String) : Logger(name) {

    companion object {
        fun configure() {
            createLogger = ::AndroidLogAdapter
        }
    }

    override fun isEnabled(level: LogLevel) =
        level != OFF && Log.isLoggable(name, level.toInt())

    override fun publish(logEvent: LogEvent) {
        logEvent.run {
            when (logLevel) {
                OFF -> Unit // don't publish anything
                TRACE -> Log.v(name, messageString, throwable)
                DEBUG -> Log.d(name, messageString, throwable)
                INFO -> Log.i(name, messageString, throwable)
                WARN -> Log.w(name, messageString, throwable)
                ERROR -> Log.e(name, messageString, throwable)
            }
        }
    }

    private fun LogLevel.toInt() = when (this) {
        OFF -> throw IllegalArgumentException("OFF")
        TRACE -> Log.VERBOSE
        DEBUG -> Log.DEBUG
        INFO -> Log.INFO
        WARN -> Log.WARN
        ERROR -> Log.ERROR
    }
}