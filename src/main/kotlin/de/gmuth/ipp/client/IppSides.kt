package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppTag

enum class IppSides : IppJobParameter {

    OneSided, TwoSidedLongEdge, TwoSidedShortEdge;

    override fun toIppAttribute() = IppAttribute("sides", IppTag.Keyword, registeredValue(name))

}