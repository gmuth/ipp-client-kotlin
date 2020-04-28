package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppTag

class IppDocumentFormat(value: String) :
        IppAttribute<String>("document-format", IppTag.MimeMediaType, value)