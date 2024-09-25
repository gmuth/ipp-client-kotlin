package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

open class IppException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    class IppAttributeNotFoundException(name: String, tag: IppTag) :
        IppException("'$name' not found in group $tag")

}