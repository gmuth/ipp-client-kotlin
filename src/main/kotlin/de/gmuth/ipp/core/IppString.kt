package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

// name or text value, optional language
data class IppString(val text: String, val language: String? = null) {

    override fun toString() = "${if (language == null) "" else "[$language] "}$text"

}