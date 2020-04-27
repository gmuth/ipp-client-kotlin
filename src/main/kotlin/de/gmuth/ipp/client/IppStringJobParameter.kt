package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppTag

open class IppStringJobParameter(val name: String, val tag: IppTag, vararg val values: String) : IppJobParameter {
    override fun toIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<String> {
        return IppAttribute(name, tag, values.toList())
    }
}

class IppJobName(value: String) : IppStringJobParameter("job-name", IppTag.NameWithoutLanguage, value)

class IppDocumentFormat(value: String) : IppStringJobParameter("document-format", IppTag.MimeMediaType, value)