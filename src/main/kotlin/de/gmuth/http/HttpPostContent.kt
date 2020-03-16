package de.gmuth.http

/**
 * Author: Gerhard Muth
 */

import java.net.URI

interface HttpPostContent {

    fun post(uri: URI, content: HttpContent): HttpResponse

}