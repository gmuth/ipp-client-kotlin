package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword

open class Media(val keyword: String) : IppAttributeBuilder {

    object ISO_A3 : Media("iso_a3_297x420mm")
    object ISO_A4 : Media("iso_a4_210x297mm")
    object ISO_A5 : Media("iso_a5_148x210mm")
    object ISO_A6 : Media("iso_a6_105x148mm")
    object NA_LEGAL : Media("na_legal_8.5x14in")
    object NA_LETTER : Media("na_letter_8.5x11in")
    object NA_LEDGER : Media("na_ledger_11x17in")

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("media", Keyword, keyword)

}