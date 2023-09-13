package de.gmuth.log

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import java.util.logging.LogManager

object Logging {
    fun configure() {
        LogManager
            .getLogManager()
            .readConfiguration(Logging::class.java.getResourceAsStream("/logging.properties"))
    }
}