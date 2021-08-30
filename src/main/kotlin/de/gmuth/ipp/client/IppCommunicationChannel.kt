package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import java.net.URI

// RFC 8011, page 26
class IppCommunicationChannel(
        val uri: URI,
        val security: String,
        val authentication: String
) {
    override fun toString() = "$uri, security=$security, authentication=$authentication"
}