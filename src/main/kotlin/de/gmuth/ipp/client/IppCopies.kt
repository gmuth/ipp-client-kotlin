package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppTag

class IppCopies(value: Int) :
        IppAttribute<Int>("copies", IppTag.Integer, value)