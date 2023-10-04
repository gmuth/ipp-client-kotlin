package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import java.net.URI

// RFC 8011, page 26
class CommunicationChannel(
    val uri: URI,
    val security: String,
    val authentication: String
) {
    override fun toString() = "$uri, security=$security, authentication=$authentication"

    companion object {
        fun getCommunicationChannelsSupported(attributes: IppAttributesGroup) = with(attributes) {
            val printerUriSupportedList = getValues<List<URI>>("printer-uri-supported")
            val uriSecuritySupportedList = getValues<List<String>>("uri-security-supported")
            val uriAuthenticationSupportedList = getValues<List<String>>("uri-authentication-supported")
            printerUriSupportedList.indices.map {
                CommunicationChannel(
                    printerUriSupportedList[it],
                    uriSecuritySupportedList[it],
                    uriAuthenticationSupportedList[it]
                )
            }
        }
    }

}