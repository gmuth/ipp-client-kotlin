package de.gmuth.ipp.cli

import java.net.URI

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: java -jar ipp-client-fat.jar <printer-uri> <file>")
        return
    }

    with(IppTool()) {
        uri = URI.create(args[0])
        filename = args[1]

        interpret(
                "OPERATION Print-Job",
                "GROUP operation-attributes-tag",
                "ATTR charset attributes-charset utf-8",
                "ATTR language attributes-natural-language en",
                "ATTR uri printer-uri \$uri",
                "FILE \$filename"
        )
    }

}