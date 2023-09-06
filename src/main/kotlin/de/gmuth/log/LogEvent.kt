package de.gmuth.log

/**
 * Copyright (c) 2023 Gerhard Muth
 */

class LogEvent(
    val logLevel: Logging.LogLevel,
    val produceMessage: MessageProducer,
    val throwable: Throwable?
) {
    val messageString by lazy { produceMessage()?.toString() }
}