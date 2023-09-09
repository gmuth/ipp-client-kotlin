package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

import java.nio.charset.Charset
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

class IppConfig(
    var userName: String? = System.getProperty("user.name"),
    var ippVersion: String = "1.1",
    var charset: Charset = Charsets.UTF_8,
    var naturalLanguage: String = "en",
) {
    //val log = getLogger(javaClass.name)

    fun log(log: Logger) = log.run {
        info { "userName: $userName" }
        info { "ippVersion: $ippVersion" }
        info { "charset: ${charset.name().lowercase()}" }
        info { "naturalLanguage: $naturalLanguage" }
    }
}