package de.gmuth.io

import de.gmuth.ipp.core.IppResponse
import java.io.File

fun File.toIppResponse(): IppResponse {
    val file = this
    return IppResponse().apply {
        read(file)
    }
}