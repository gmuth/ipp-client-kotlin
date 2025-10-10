import de.gmuth.ipp.client.IppClient
import de.gmuth.ipp.client.IppPrinter
import de.gmuth.log.Logging
import java.net.URI
import java.util.logging.Level

// https://github.com/gmuth/ipp-client-kotlin/issues/40

fun main() {
    Logging.configure()
    val ippClientForNAT = IppClient().apply {
        onExchangeOverrideRequestPrinterOrJobUri = URI.create("ipp://192.168.2.145/route-via-nat-or-reverse-proxy")
        onExchangeLogRequestAndResponseWithLevel = Level.INFO
        config.userName = null // Omit 'requesting-user-name' attribute
    }
    IppPrinter(URI.create("ipp://direct-route-only/ipp/print"), ippClient = ippClientForNAT)
}
