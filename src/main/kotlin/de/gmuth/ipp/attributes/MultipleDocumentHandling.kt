package de.gmuth.ipp.attributes

/**
 * Copyright (c) 2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag.Keyword

enum class MultipleDocumentHandling(private val keyword: String) : IppAttributeBuilder {

    SeparateDocumentsCollatedCopies("separate-documents-collated-copies"),
    SeparateDocumentsUncollatedCopies("separate-documents-uncollated-copies"),
    SingleDocument("single-document");

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) =
        IppAttribute("multiple-document-handling", Keyword, keyword)
}