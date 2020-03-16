package de.gmuth.cli

/**
 * Author: Gerhard Muth
 */

import de.gmuth.ipp.IppClient
import java.io.File
import java.io.FileInputStream
import java.net.URI

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: java -jar ippclient.jar <printer-uri> <file>")
        return
    }
    val uri = URI.create(args[0])
    val file = File(args[1])
    IppClient(uri).printDocument(FileInputStream(file))
}