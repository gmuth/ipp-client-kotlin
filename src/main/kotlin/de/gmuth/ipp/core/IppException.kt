package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

open class IppException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    class IppAttributeNotFoundException(attributeName: String, tag: IppTag) :
        IppException("attribute '$attributeName' not found in ${tag.name} group")

}