package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

fun Int.toPluralString(description: String) = "${toString()} $description${if (this == 1) "" else "s"}"
