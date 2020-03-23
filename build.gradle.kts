import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.41"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

defaultTasks("clean", "shadowJar")

tasks.withType<ShadowJar>() {
    archiveBaseName.set("ippclient")
    archiveClassifier.set("")
    manifest {
        attributes(mapOf("Main-Class" to "de.gmuth.ipp.cli.PrintDocumentKt"))
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}