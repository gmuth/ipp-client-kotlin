package de.gmuth.log

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import java.io.OutputStream
import java.util.logging.ConsoleHandler
import java.util.logging.Level

class ConsoleHandler(outputStream: OutputStream = System.out) : ConsoleHandler() {
    init {
        super.setOutputStream(outputStream)
        level = Level.ALL
    }
}