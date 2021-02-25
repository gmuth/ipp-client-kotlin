package de.gmuth.ipp.cli

import java.net.URI

fun main() {
    with(IppTool()) {
        uri = URI.create("ipp://localhost:8632/printers/laser")
        filename = "tool/A4-blank.pdf"

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