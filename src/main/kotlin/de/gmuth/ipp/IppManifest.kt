package de.gmuth.ipp

import java.security.MessageDigest

/**
 * Copyright (c) 2025-2026 Gerhard Muth
 */

class IppManifest {
    companion object {
        private val instance = IppManifest::class.java
            .getResourceAsStream("/META-INF/MANIFEST.MF")
            .use { Manifest(it) }
        val mainAttributes = instance.mainAttributes
        val mavenArtifactName = mainAttributes.getValue("Maven-Artifact-Name")
        val mavenArtifactGroup = mainAttributes.getValue("Maven-Artifact-Group")
        val mavenArtifactVersion = mainAttributes.getValue("Maven-Artifact-Version")
        val mavenCoordinates = "$mavenArtifactGroup:$mavenArtifactName:$mavenArtifactVersion"

        fun checksum() = MessageDigest.getInstance("SHA-256")
            .digest(mavenCoordinates.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}