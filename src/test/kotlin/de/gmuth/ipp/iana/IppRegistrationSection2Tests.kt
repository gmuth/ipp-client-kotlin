package de.gmuth.ipp.iana

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.attributes.ColorMode
import de.gmuth.ipp.attributes.DocumentFormat
import de.gmuth.ipp.attributes.TemplateAttributes.jobName
import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributeBuilder
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.Keyword
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
            .map { it.name }
            .filter { it.matchesNot(".*(deprecated|obsolete|extension).*") }
            .distinct()
            .sorted()
            .onEach { println("\"$it\",") }
            //.reduce { list, element -> "$list,$element" }
            .forEach {
                assertEquals(Operation, selectGroupForAttribute(it), "operation group required for $it")
            }
    }

    @Test
    fun operationGroupAttributes() {
        assertEquals(Operation, selectGroupForAttribute(jobName("myjob")))
        assertEquals(Operation, selectGroupForAttribute(DocumentFormat("pdf")))
    }

    @Test
    fun unknownAttributes() {
        assertEquals(IppTag.Job, selectGroupForAttribute(IppAttribute("Ink", Keyword, "MONO")))
        assertEquals(IppTag.Job, selectGroupForAttribute(ColorMode("mono")))
    }

    fun selectGroupForAttribute(attributeBuilder: IppAttributeBuilder) = selectGroupForAttribute(
        attributeBuilder.buildIppAttribute(
            IppAttributesGroup(IppTag.Printer).apply {
                attribute("output-mode-supported", Keyword, ColorMode.Monochrome.keyword)
            }
        ).name
    )
}