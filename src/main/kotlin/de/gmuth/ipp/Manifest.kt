package de.gmuth.ipp

import de.gmuth.ipp.client.IppClient
import java.util.jar.Manifest

/**
 * Copyright (c) 2025 Gerhard Muth
 */

class Manifest {
    companion object {
        private val instance = Manifest(IppClient::class.java.getResourceAsStream("/META-INF/MANIFEST.MF"))
        val mainAttributes = instance.mainAttributes
        val mavenArtifactName = mainAttributes.getValue("Maven-Artifact-Name")
        val mavenArtifactGroup = mainAttributes.getValue("Maven-Artifact-Group")
        val mavenArtifactVersion = mainAttributes.getValue("Maven-Artifact-Version")
    }
}