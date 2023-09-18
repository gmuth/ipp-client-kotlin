import org.jetbrains.dokka.gradle.DokkaTask

// how to build? run ./gradlew
// where is the jar? build/lib/ipp-client-kotlin...jar

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.22"
    id("org.jetbrains.dokka") version "1.7.20"
    id("org.sonarqube") version "4.3.0.3225" // https://plugins.gradle.org/plugin/org.sonarqube
    //id("org.sonarqube") version "3.5.0.2730" // supports java 8, dropped with 4.1
    id("maven-publish")
    id("signing")
    id("jacoco")
}

group = "de.gmuth"
version = "2.5"

repositories {
    mavenCentral()
}

// update gradle wrapper
// ./gradlew wrapper --gradle-version 7.6.2

//java {
//    registerFeature("slf4jSupport") {
//        usingSourceSet(sourceSets["main"])
//    }
//}

// -> gradle.properties
val slf4jVersion: String by project
val androidVersion: String by project

dependencies {
    //implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    //implementation("org.jetbrains.kotlin:kotlin-stdlib")
    //implementation("org.jetbrains.kotlin:kotlin-reflect")
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    //"slf4jSupportImplementation"("org.slf4j:slf4j-api:1.7.32") // pom.xml: scope=compile, optional=true
    compileOnly("org.slf4j:slf4j-api:$slf4jVersion")
    compileOnly("com.google.android:android:$androidVersion") // android.util.Log

    testRuntimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
    //testRuntimeOnly("ch.qos.logback:logback-classic:1.2.3")
}

// gradlew clean -x test build publishToMavenLocal
defaultTasks("assemble")

//java {
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(8))
//    }
//}

tasks.compileKotlin {
    kotlinOptions {
        // If 1.6 is required you also have to configure gradle plugin org.jetbrains.kotlin.jvm version 1.6
        jvmTarget = "1.8"
    }
}

// avoid warnings "jvm target compatibility should be set to the same Java version."
tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "11"
    }
}
tasks.compileJava {
    sourceCompatibility = tasks.compileKotlin.get().kotlinOptions.jvmTarget
    targetCompatibility = tasks.compileKotlin.get().kotlinOptions.jvmTarget
}

//tasks.withType<Jar> {
//    archiveBaseName.set("ipp-client")
//    archiveClassifier.set("")
//}

// ================= PUBLISHING ================

val repo = System.getProperty("repo")
publishing {
    repositories {
        if (repo == "github") {
            // Github Packages:
            // gradlew -Drepo=github publish
            println("> maven repo github")
            maven {
                name = "GitHubPackages" // Must match regex [A-Za-z0-9_\-.]+.
                url = uri("https://maven.pkg.github.com/gmuth/ipp-client-kotlin")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                    password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
        // Maven Central:
        // https://central.sonatype.org/publish/release/
        // gradlew -Drepo=sonatype publish
        // https://s01.oss.sonatype.org/#stagingRepositories (not Safari)
        // "Close" and wait "for rule evalutaion"
        // "Release"
        if (repo == "sonatype") {
            println("> maven repo sonatype")
            maven {
                name = "Sonatype"
                //val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                //val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                //url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                val host = "https://s01.oss.sonatype.org"
                val path = if (version.toString().endsWith("SNAPSHOT")) "/content/repositories/snapshots/"
                else "/service/local/staging/deploy/maven2/"
                url = uri(host.plus(path))
                println("> publish.url: $url")
                credentials {
                    username = project.findProperty("ossrh.username") as String?
                    password = project.findProperty("ossrh.password") as String?
                }
            }
        }
    }

    publications {
        create<MavenPublication>("ippclient") {
            from(components["java"])
            pom {
                name.set("ipp client library")
                description.set("A client implementation of the ipp protocol, RFCs 8010, 8011, 3995 and 3996")
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
                scm {
                    connection.set("scm:git:git://github.com/gmuth/ipp-client-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/gmuth/ipp-client-kotlin.git")
                    url.set("https://github.com/gmuth/ipp-client-kotlin")
                }
            }
        }
    }
}

java {
    withSourcesJar()
}

repo?.run { // tasks for publishing on maven repo
    // ====== signing ======
    // set gradle.properties
    // signing.keyId
    // signing.password
    // signing.secretKeyRingFile
    // gradle signIppclientPublication
    signing {
        sign(publishing.publications["ippclient"])
    }
    // ======  produce sources.jar and javadoc.jar ======
    java {
        //withSourcesJar()
        withJavadocJar()
    }
    // configure task javadocJar to take javadoc generated by dokkaJavadoc
    tasks.named<Jar>("javadocJar") {
        from(tasks.named<DokkaTask>("dokkaJavadoc"))
    }
}

// ====== analyse code with SonarQube ======

// required for sonarqube code coverage
// https://docs.sonarqube.org/latest/analysis/test-coverage/java-test-coverage
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    // https://stackoverflow.com/questions/67725347/jacoco-fails-on-gradle-7-0-2-and-kotlin-1-5-10
    //version = "0.8.7"
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }
}

// gradle test jacocoTestReport sonar
// https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/
// configure token with 'publish analysis' permission in file ~/.gradle/gradle.properties:
// systemProp.sonar.login=<token>
sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectKey", "gmuth_ipp-client-kotlin")
        property("sonar.organization", "gmuth")
        //property("sonar.verbose", "true")
        //property("sonar.junit.reportPaths", "build/test-results/test")
        //property("sonar.jacoco.reportPaths", "build/jacoco/test.exec")
        //property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/")
    }
}

tasks.sonar {
    dependsOn(tasks.jacocoTestReport) // for coverage
}

tasks.register("fatJar", Jar::class) {
    description = "Generates a fat jar for this project."
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    manifest.attributes["Main-Class"] = "CopyNewJobsKt"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map(::zipTree))
    with(tasks.jar.get())
}