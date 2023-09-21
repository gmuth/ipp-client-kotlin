package de.gmuth.log

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import java.util.*
import java.util.logging.LogManager

object Logging {
    fun configure() {
        Locale.setDefault(Locale.ENGLISH) // -Duser.language=en
        LogManager.getLogManager()
            .readConfiguration(Logging::class.java.getResourceAsStream("/logging.properties"))
    }
}