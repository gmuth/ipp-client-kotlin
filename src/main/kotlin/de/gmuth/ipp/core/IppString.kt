package de.gmuth.ipp.core;

/**
 * Copyright (c) 2020 Gerhard Muth
 */

/**
 * name or text value with or without language
 */
class IppString(val string: String, val language: String? = null) {

    override fun toString() = "${if (language == null) "" else "[$language] "}$string"

    fun length() = 2 + string.length + if (language == null) 0 else 2 + language.length

}