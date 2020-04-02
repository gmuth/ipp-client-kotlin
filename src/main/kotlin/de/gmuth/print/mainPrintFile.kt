package de.gmuth.print

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.print.IppPrintService
import java.io.File
import java.net.URI

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: java -jar ippclient.jar <printer-uri> <file>")
        return
    }
    val uri = URI.create(args[0])
    val file = File(args[1])

    val printService = IppPrintService(uri)
    printService.printFile(file)

}