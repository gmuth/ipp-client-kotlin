package de.gmuth.ipp.cli

/**
 * Author: Gerhard Muth
 */

import de.gmuth.ipp.core.IppClient
import de.gmuth.ipp.IppPrintJobOperation
import de.gmuth.ipp.core.IppMessage
import java.io.File
import java.io.FileInputStream
import java.net.URI

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: java -jar ippclient.jar <printer-uri> <file>")
        return
    }
    val printerURI = URI.create(args[0])
    val file = File(args[1])

    IppMessage.verbose = true
    val ippClient = IppClient(printerURI)
    val ippRequest = IppPrintJobOperation(printerURI)
    val ippResponse = ippClient.exchangeIpp(ippRequest, FileInputStream(file))

    if (!ippResponse.status.successfulOk()) {
        println("printing to $printerURI failed")
    }
}