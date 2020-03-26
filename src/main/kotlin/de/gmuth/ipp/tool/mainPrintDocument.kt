package de.gmuth.ipp.tool

import java.io.File
import java.net.URI


fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: java -jar ipptool.jar <printer-uri> <file>")
        return
    }
    val printerUri = URI.create(args[0])
    val file = File(args[1])

    with(IppTool(printerUri)) {
        this.file = file
        run(
                "OPERATION Print-Job",
                "GROUP operation-attributes-tag",
                "ATTR charset attributes-charset utf-8",
                "ATTR language attributes-natural-language en",
                "ATTR uri printer-uri $printerUri"
        )
    }

}