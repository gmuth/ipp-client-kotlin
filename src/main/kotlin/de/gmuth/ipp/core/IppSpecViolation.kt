package de.gmuth.ipp.core

import java.io.IOException

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppSpecViolation(override val message: String) : IOException(message)