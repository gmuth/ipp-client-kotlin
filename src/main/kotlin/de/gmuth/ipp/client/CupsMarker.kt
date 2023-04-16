package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.log.Logging

// https://www.cups.org/doc/spec-ipp.html
class CupsMarker(
    val type: String,
    val name: String,
    val level: Int,
    val lowLevel: Int,
    val highLevel: Int,
    val colorCode: String
) {
    companion object {
        val log = Logging.getLogger {}
    }

    val color: Color = Color.fromString(colorCode)

    fun levelPercent() = 100 * level / highLevel
    fun levelIsLow() = level <= lowLevel

    override fun toString() = "%-10s %3d %% %5s %-6s %-7s %s".format(
        color, levelPercent(), if (levelIsLow()) "(low)" else "", type, colorCode, name
    )

    enum class Color(val code: String) {
        NONE("none"),
        CYAN("#00FFFF"),
        BLACK("#000000"),
        YELLOW("#FFFF00"),
        MAGENTA("#FF00FF"),
        TRI_COLOR("#00FFFF#FF00FF#FFFF00"), // Cyan, Magenta, Yellow
        UNKNOWN("?");

        companion object {
            fun fromString(code: String) = values().find { it.code == code }
                ?: UNKNOWN.apply { log.warn { "unknown color code: $code" } }
        }
    }

}