package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword

class Media(val keyword: String) : IppAttributeBuilder {

    companion object {
        @JvmField
        val ISO_A3 = Media("iso_a3_297x420mm")

        @JvmField
        val ISO_A4 = Media("iso_a4_210x297mm")

        @JvmField
        val ISO_A5 = Media("iso_a5_148x210mm")

        @JvmField
        val ISO_A6 = Media("iso_a6_105x148mm")

        @JvmField
        val NA_LEGAL = Media("na_legal_8.5x14in")

        @JvmField
        val NA_LETTER = Media("na_letter_8.5x11in")

        @JvmField
        val NA_LEDGER = Media("na_ledger_11x17in")
    }

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("media", Keyword, keyword)

}