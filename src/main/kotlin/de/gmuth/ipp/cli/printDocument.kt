package de.gmuth.ipp.cli

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppClient
import de.gmuth.ipp.core.IppMessage
import de.gmuth.ipp.printDocument
import java.io.File
import java.io.FileInputStream
import java.net.URI

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: java -jar ippclient.jar <printer-uri> <file>")
        return
    }
    val printerUri = URI.create(args[0])
    val file = File(args[1])

    IppMessage.verbose = true
    IppClient(printerUri).printDocument(FileInputStream(file))
}