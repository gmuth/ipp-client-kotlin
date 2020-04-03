package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.File
import java.net.URI

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: java -jar ippclient.jar <printer-uri> <file>")
        return
    }
    val uri = URI.create(args[0])
    val file = File(args[1])

    with(IppClient()) {
        val printJob = IppPrintJob(uri, file)
        val job = sendPrintJob(printJob, waitForTermination = true)
        job.logDetails()
    }

}