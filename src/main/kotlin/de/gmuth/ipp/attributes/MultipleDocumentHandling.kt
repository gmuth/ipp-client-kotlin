package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword

enum class MultipleDocumentHandling(val keyword: String) : IppAttributeBuilder {

    SeparateDocumentsUncollatedCopies("separate-documents-uncollated-copies"),
    SeparateDocumentsCollatedCopies("separate-documents-collated-copies"),
    SingleDocumentNewSheet("single-document-new-sheet"),
    SingleDocument("single-document");

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("multiple-document-handling", Keyword, keyword)
}