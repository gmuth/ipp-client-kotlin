//import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.10"
    //id("com.github.johnrengelman.shadow") version "5.2.0"
    id("jacoco")
    id("org.sonarqube") version "3.0"
    id("maven-publish")
}

group = "de.gmuth.ipp"
version = "2.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

//java {
//    registerFeature("slf4jSupport") {
//        usingSourceSet(sourceSets["main"])
//    }
//}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    //implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    //"slf4jSupportImplementation"("org.slf4j:slf4j-api:1.7.30") // pom.xml: scope=compile, optional=true
    compileOnly("org.slf4j:slf4j-api:1.7.30")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.30")
}

// gradlew clean -x test build publishToMavenLocal
defaultTasks("clean", "build", "publishToMavenLocal")

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.4"
    jvmTarget = "1.6"
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions {
//        jvmTarget = "1.6"
//    }
//}

tasks.withType<Jar> {
    archiveBaseName.set("ipp-client")
    archiveClassifier.set("")
}

//tasks.withType<ShadowJar> {
//    archiveBaseName.set("ipp-client-fat")
//    archiveClassifier.set("")
//    manifest {
//        attributes(mapOf("Main-Class" to "de.gmuth.ipp.tool.PrintFileKt"))
//    }
//}

//tasks {
//    build {
//        dependsOn(shadowJar)
//    }
//}

// do NOT publish from your developer host!
// to release: 1. remove SNAPSHOT from version; 2. commit & push; 3. check github workflow results
// if the workflow tries to publish the same release again you'll get: "Received status code 409 from server: Conflict"

publishing {

    repositories {
        maven {
            name = "GitHubPackages" // Must match regex [A-Za-z0-9_\-.]+.
            url = uri("https://maven.pkg.github.com/gmuth/ipp-client-kotlin")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("ipp-client") {
            from(components["java"])
            pom {
                name.set("ipp client library")
                url.set("https://github.com/gmuth/ipp-client-kotlin")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://raw.githubusercontent.com/gmuth/ipp-client-kotlin/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("gmuth")
                        name.set("Gerhard Muth")
                        email.set("gerhard.muth@gmx.de")
                    }
                }
            }
        }
    }

}

// ====== SONAR CODE ANALYSIS ======

// required for sonarqube code coverage
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = true
    }
}

// gradle test jacocoTestReport sonarqube
// https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/
// configure token with 'publish analysis' permission in file ~/.gradle/gradle.properties:
// systemProp.sonar.login=<token>
sonarqube {
    properties {
        property("sonar.projectKey", "gmuth_ipp-client-kotlin")
        property("sonar.organization", "gmuth")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}