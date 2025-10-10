package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2025 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString

class PrinterFirmware(
    val name: String,
    val stringVersion: String,
    val version: String, // octetString
    val patches: String? = null
) {
    override fun toString() = "$name: $stringVersion" + if (patches == null) "" else "(patch $patches)"

    companion object {
        fun getPrinterFirmware(attributes: IppAttributesGroup): List<PrinterFirmware> = with(attributes) {
            val nameList = getValues<List<IppString>>("printer-firmware-name")
            val stringVersionList = getValues<List<IppString>>("printer-firmware-string-version")
            val versionList = getValues<List<String>>("printer-firmware-version") // octetString
            val patchesList = getValuesOrNull<List<IppString>>("printer-firmware-patches")
            nameList.indices.map {
                PrinterFirmware(
                    nameList[it].text,
                    versionList[it],
                    stringVersionList[it].text,
                    patchesList?.get(it)?.text
                )
            }
        }
    }
}
