package de.gmuth.ipp.cli

import de.gmuth.ipp.client.IppPrinter
import java.io.File
import java.net.URI

// java -cp ipp-client-fat-...jar de.gmuth.ipp.cli.SavePrinterAttributes ipp://colorjet.local
// file: ..../HP_LaserJet_100_colorMFP_M175nw.txt

class SavePrinterAttributes {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val uri = URI.create(args[0])
            println("uri: ${uri}")
            val ippPrinter = IppPrinter(uri)
            val printerMakeAndModel = ippPrinter.makeAndModel.text.replace("\\s+".toRegex(), "_")

            val txtFile = File(printerMakeAndModel.plus(".txt"))
            txtFile.writeText("# ${uri}\n")
            println("txt file: ${txtFile.absolutePath}")
            for (attribute in ippPrinter.attributes.values) {
                txtFile.appendText("${attribute}\n")
            }

            val binFile = File(printerMakeAndModel.plus(".bin"))
            println("bin file: ${binFile.absolutePath}")
            ippPrinter.ippClient.writeLastIppResponse(binFile)
        }
    }
}