package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.IOException

open class IppException(
        message: String,
        cause: Throwable? = null
) : IOException(message, cause)