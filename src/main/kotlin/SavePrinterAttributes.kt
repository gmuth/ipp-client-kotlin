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
            val filename = ippPrinter.makeAndModel.string.replace("\\s+".toRegex(), "_").plus(".txt")
            val file = File(filename)
            file.writeText("# ${uri}\n")
            for (attribute in ippPrinter.attributes.values) {
                file.appendText("${attribute}\n")
            }
            println("file: ${file.absolutePath}")
        }
    }
}
