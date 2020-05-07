import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.41"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("maven-publish")
}

group = "de.gmuth.ipp0"
version = "1.2.3-SNAPSHOT"

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

defaultTasks("clean", "build")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Jar> {
    archiveBaseName.set("ipp-client")
    archiveClassifier.set("")
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("ipp-client-fat")
    archiveClassifier.set("")
    manifest {
        attributes(mapOf("Main-Class" to "de.gmuth.ipp.tool.PrintFileKt"))
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/gmuth/m2repo")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    // Received status code 422 from server: Unprocessable Entity
    // group or artifact exists
    // group must be unique (and not used in other repo packages)
    // in public repos you can not delete artifacts!
}
