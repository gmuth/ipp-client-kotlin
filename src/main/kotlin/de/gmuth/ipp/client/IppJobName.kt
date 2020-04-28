package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppTag

class IppJobName(value: String) :
        IppAttribute<String>("job-name", IppTag.NameWithoutLanguage, value)