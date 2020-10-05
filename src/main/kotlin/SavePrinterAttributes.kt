import de.gmuth.ipp.client.IppPrinter
import java.io.File
import java.net.URI

// java -cp ipp-client-fat-1.5-SNAPSHOT.jar SavePrinterAttributes  ipp://colorjet.local
// file: ..../HP_LaserJet_100_colorMFP_M175nw.txt

class SavePrinterAttributes {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val uri = URI.create(args[0])
            println("uri: ${uri}")
            val ippPrinter = IppPrinter(uri)
            val filename = ippPrinter.makeAndModel.string.replace("\\s+".toRegex(), "_")

            val txtFile = File(filename.plus(".txt"))
            txtFile.writeText("# ${uri}\n")
            println("txt file: ${txtFile.absolutePath}")
            for (attribute in ippPrinter.attributes.values) {
                txtFile.appendText("${attribute}\n")
            }

            val binFile = File(filename.plus(".bin"))
            println("bin file: ${binFile.absolutePath}")
            ippPrinter.ippClient.writeLastIppResponse(binFile)
        }
    }
}
