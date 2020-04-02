package de.gmuth.print

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.File

interface PrintService {

    enum class ColorMode { Auto, Color, Monochrome }

    fun printFile(
            file: File,
            colorMode: ColorMode = ColorMode.Auto,
            waitForTermination: Boolean = true
    )

}