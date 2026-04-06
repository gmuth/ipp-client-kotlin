package de.gmuth.ipp.client

/**
 * Copyright (c) 2026 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString
import java.util.logging.Level
import java.util.logging.Logger

class CupsPPD(
    val attributes: IppAttributesGroup
) {
    val name: IppString // nameWithoutLanguage
        get() = attributes.getValue("ppd-name")

    val naturalLanguage: List<String> // 1setOf naturalLanguage
        get() = attributes.getValues("ppd-natural-language")

    val make: IppString // textWithoutLanguage
        get() = attributes.getValue("ppd-make")

    val makeAndModel: IppString // textWithoutLanguage
        get() = attributes.getValue("ppd-make-and-model")

    val deviceId: IppString // textWithoutLanguage
        get() = attributes.getValue("ppd-device-id")

    val product: List<IppString> // 1setOf textWithoutLanguage
        get() = attributes.getValues("ppd-product")

    val psVersion: IppString // textWithoutLanguage
        get() = attributes.getValue("ppd-psversion")

    val type: String // keyword
        get() = attributes.getValue("ppd-type")

    val modelNumber: Int
        get() = attributes.getValue("ppd-model-number")

    fun log(logger: Logger, level: Level = Level.INFO) {
        logger.log(level) { "PPD" }
        logger.log(level) { "  name: $name" }
        logger.log(level) { "  naturalLanguage: ${naturalLanguage.joinToString(", ")}" }
        logger.log(level) { "  make: $make" }
        logger.log(level) { "  makeAndModel: $makeAndModel" }
        logger.log(level) { "  deviceId: $deviceId" }
        logger.log(level) { "  product: ${product.joinToString(", ")}" }
        logger.log(level) { "  type: $type" }
        logger.log(level) { "  psVersion: $psVersion" }
        logger.log(level) { "  modelNumber: $modelNumber" }
    }

    override fun toString() = "$name (type=$type)"

}