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
                    values().find { it.code == code } ?: throw IllegalArgumentException(String.format("color code %s", code))
        }
    }

    class List(attributes: IppAttributesGroup) : ArrayList<CupsMarker>() {
        init {
            with(attributes) {
                // https://www.cups.org/doc/spec-ipp.html
                val types: kotlin.collections.List<String> = getValues("marker-types")
                val names: kotlin.collections.List<IppString> = getValues("marker-names")
                val colors: kotlin.collections.List<IppString> = getValues("marker-colors")
                val levels: kotlin.collections.List<Int> = getValues("marker-levels")
                val lowLevels: kotlin.collections.List<Int> = getValues("marker-low-levels")
                val highLevels: kotlin.collections.List<Int> = getValues("marker-high-levels")

                for ((index, type) in types.withIndex()) {
                    add(CupsMarker(
                            type,
                            names[index].text,
                            colors[index].text,
                            levels[index],
                            lowLevels[index],
                            highLevels[index]
                    ))
                }
            }
        }
    }

}