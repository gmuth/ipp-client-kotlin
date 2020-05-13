package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// name or text value with or without language

data class IppString(
        val string: String,
        val language: String? = null
) {
    override fun toString() = "$string${if (language == null) "" else "[$language]"}"
}