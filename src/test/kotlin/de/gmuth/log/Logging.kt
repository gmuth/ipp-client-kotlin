package de.gmuth.log

import java.io.FileInputStream
import java.util.logging.LogManager
import java.util.logging.Logger

/**
 * Copyright (c) 2023 Gerhard Muth
 */

fun LogManager.readConfigurationFile(filename: String = "logging.properties") =
    readConfiguration(FileInputStream(filename))
        .also { println("configured logging by file: $filename") }

fun LogManager.readConfigurationResource(resource: String = "/logging.properties") =
    readConfiguration(Logging::class.java.getResourceAsStream(resource))
        .also { println("configured logging by resource: $resource") }

object Logging {

    init {
        LogManager.getLogManager().readConfigurationResource()
    }

    fun getLogger(name: String) =
        Logger.getLogger(name)

    fun getLogger(noOperation: () -> Unit) =
        Logger.getLogger(noOperation.javaClass.enclosingClass.name)

}