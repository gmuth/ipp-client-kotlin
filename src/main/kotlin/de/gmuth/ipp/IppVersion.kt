package de.gmuth.ipp

/**
 * Author: Gerhard Muth
 */

// Int for convenience only, each number will is encoded as byte
class IppVersion(val major: Int = 1, val minor: Int = 1) {

    override fun toString(): String {
        return String.format("%d.%d", major, minor)
    }

}