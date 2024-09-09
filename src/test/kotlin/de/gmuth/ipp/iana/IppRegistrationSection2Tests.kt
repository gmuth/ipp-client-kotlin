package de.gmuth.ipp.iana

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.attributes.DocumentFormat
import de.gmuth.ipp.attributes.TemplateAttributes.jobName
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.Operation
import de.gmuth.ipp.iana.IppRegistrationsSection2.selectGroupForAttribute
import de.gmuth.log.Logging
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals

class IppRegistrationSection2Tests {

    init {
        Logging.configure()
    }

    val logger = Logger.getLogger("IanaRegistrationSec2")
    private fun CharSequence.matchesNot(regexString: String) = !matches(regexString.toRegex())

    @Test
    fun listOperationAttributes() {
        IppRegistrationsSection2
            .attributesMap
            .values
            .filter { it.collection == "Operation" }
            .map { "\"${it.name}\"" }
            .filter { it.matchesNot(".*(deprecated|obsolete).*") }
            .distinct()
            .sorted()
            .forEach { println("$it,") }
            //.reduce { list, element -> "$list,$element" }
    }

    @Test
    fun operationGroupAttributes() {
        assertEquals(Operation, selectGroupForAttribute(jobName("myjob")))
        assertEquals(Operation, selectGroupForAttribute(DocumentFormat("pdf")))
    }

    fun selectGroupForAttribute(attributeBuilder: IppAttributeBuilder) =
        selectGroupForAttribute(attributeBuilder.buildIppAttribute(IppAttributesGroup(IppTag.Printer)).name)

}