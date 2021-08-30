package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import java.net.URI

class IppCommunicationChannel(
        val uri: URI,
        val security: String,
        val authentication: String
) {
    override fun toString() = "$uri, security=$security, authentication=$authentication"
}