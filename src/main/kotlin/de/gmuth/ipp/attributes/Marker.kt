package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString
import java.util.logging.Logger.getLogger

// https://www.cups.org/doc/spec-ipp.html
class Marker(
    val type: String,
    val name: String,
    val level: Int,
    val lowLevel: Int,
    val highLevel: Int,
    val colorCode: String
) {
    val color: Color = Color.fromString(colorCode)

    fun levelPercent() = 100 * level / highLevel
    fun levelIsLow() = level < lowLevel

    override fun toString() = "%-10s %3d %% %5s %-12s %-8s %s"
        .format(color, levelPercent(), if (levelIsLow()) "(low)" else "", type, colorCode, name)

    enum class Color(val code: String) {
        NONE("NONE"),
        CYAN("#00FFFF"),
        BLACK("#000000"),
        YELLOW("#FFFF00"),
        MAGENTA("#FF00FF"),
        TRI_COLOR("#00FFFF#FF00FF#FFFF00"), // Cyan, Magenta, Yellow
        UNKNOWN("#?");

        companion object {
            val log = getLogger(Color::class.java.name)
            fun fromString(code: String) = values().find { it.code == code.uppercase() }
                ?: UNKNOWN.apply { log.warning { "Unknown color code: $code" } }
        }
    }

    companion object {
        fun getMarkers(attributes: IppAttributesGroup): Collection<Marker> = with(attributes) {
            val types = getValues<List<String>>("marker-types")
            val names = getValues<List<IppString>>("marker-names")
            val levels = getValues<List<Int>>("marker-levels")
            val lowLevels = getValues<List<Int>>("marker-low-levels")
            val highLevels = getValues<List<Int>>("marker-high-levels")
            val colors = getValues<List<IppString>>("marker-colors")
            types.indices.map {
                Marker(
                    types[it],
                    names[it].text,
                    levels[it],
                    lowLevels[it],
                    highLevels[it],
                    colors[it].text
                )
            }
        }
    }
}