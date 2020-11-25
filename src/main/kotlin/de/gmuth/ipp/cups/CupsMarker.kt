package de.gmuth.ipp.cups

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString

class CupsMarker(
        val type: String,
        val name: String,
        val colorCode: String,
        val level: Int,
        val lowLevel: Int,
        val highLevel: Int
) {
    val color: Color = Color.fromString(colorCode)

    fun levelPercent() = 100 * level / highLevel
    fun levelIsLow() = level <= lowLevel

    override fun toString() =
            String.format(
                    "%-10s %3d %% %5s %-6s %-7s %s",
                    color, levelPercent(),
                    if (levelIsLow()) "(low)" else "",
                    type, colorCode, name
            )

    enum class Color(val code: String) {
        NONE("none"),
        CYAN("#00FFFF"),
        BLACK("#000000"),
        YELLOW("#FFFF00"),
        MAGENTA("#FF00FF");

        companion object {
            fun fromString(code: String): Color =
                    values().firstOrNull { it.code == code } ?: throw IllegalArgumentException(String.format("color code %s", code))
        }
    }

    class List(attributes: IppAttributesGroup) : ArrayList<CupsMarker>() {
        init {
            with(attributes) {
                // https://www.cups.org/doc/spec-ipp.html
                val types = get("marker-types")!!.values
                val names = get("marker-names")!!.values
                val colors = get("marker-colors")!!.values
                val levels = get("marker-levels")!!.values
                val lowLevels = get("marker-low-levels")!!.values
                val highLevels = get("marker-high-levels")!!.values

                for ((index, name) in names.withIndex()) {
                    val marker = CupsMarker(
                            name = (name as IppString).text,
                            type = types[index] as String,
                            colorCode = (colors[index] as IppString).text,
                            level = levels[index] as Int,
                            lowLevel = lowLevels[index] as Int,
                            highLevel = highLevels[index] as Int
                    )
                    add(marker)
                }
            }
        }
    }

}