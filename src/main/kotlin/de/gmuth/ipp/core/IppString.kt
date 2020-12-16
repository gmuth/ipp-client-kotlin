package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// name or text value with or without language

data class IppString(val text: String, val language: String? = null) {

    override fun toString() = "$text${if (language == null) "" else "[$language]"}"

    fun expectLanguageOrThrow() {
        if (language == null) throw IppException("expected IppString with language")
    }

}

fun String.toIppString() = IppString(this)