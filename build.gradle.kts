import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.32" // 1.4.32
    id("jacoco")
    id("org.sonarqube") version "3.3" // 3.2.0
    id("maven-publish")
}

group = "de.gmuth.ipp"
version = "2.3-SNAPSHOT"

repositories {
    mavenCentral()
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
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    //"slf4jSupportImplementation"("org.slf4j:slf4j-api:1.7.30") // pom.xml: scope=compile, optional=true
    compileOnly("org.slf4j:slf4j-api:1.7.+")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.+")
}

// gradlew clean -x test build publishToMavenLocal
defaultTasks("clean", "build", "publishToMavenLocal")

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.5"
    // JVM target 1.6 is deprecated and will be removed in a future release. Please migrate to JVM target 1.8 or above
    jvmTarget = "1.6" // keep as long as we can to support older android versions
    // jdkHome = "path_to_jdk_1.6"
}

tasks.withType<Jar> {
    archiveBaseName.set("ipp-client")
    archiveClassifier.set("")
}

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
    // https://stackoverflow.com/questions/67725347/jacoco-fails-on-gradle-7-0-2-and-kotlin-1-5-10
    version = "0.8.7"
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }
}

// gradle test jacocoTestReport sonarqube
// https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/
// configure token with 'publish analysis' permission in file ~/.gradle/gradle.properties:
// systemProp.sonar.login=<token>
// warning: The Report.destination property has been deprecated. This is scheduled to be removed in Gradle 8.0.
sonarqube {
    properties {
        property("sonar.projectKey", "gmuth_ipp-client-kotlin")
        property("sonar.organization", "gmuth")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}