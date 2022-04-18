package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.log.Logging
import java.nio.charset.Charset

class IppConfig(
        var userName: String? = System.getProperty("user.name"),
        var ippVersion: String = "1.1",
        var charset: Charset = Charsets.UTF_8,
        var naturalLanguage: String = "en",
        var getPrinterAttributesOnInit: Boolean = true,
        // job-template, printer-description, all, media-col-database (not included in all!)
        var requestedAttributesOnInit: List<String>? = null
) {
    companion object {
        val log = Logging.getLogger {}
    }

    fun logDetails() {
        log.info { "userName: $userName" }
        log.info { "ippVersion: $ippVersion" }
        log.info { "charset: ${charset.name().lowercase()}" }
        log.info { "naturalLanguage: $naturalLanguage" }
        log.info { "getPrinterAttributesOnInit: $getPrinterAttributesOnInit" }
    }
}