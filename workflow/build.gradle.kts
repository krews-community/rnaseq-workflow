import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.3.31"
    id("application")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "4.0.2"
}

group = "com.genomealmanac.rnaseq"
version = "0.1.7"
val artifactID = "rnaseq-workflow"

repositories {
    jcenter()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/weng-lab/krews")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("io.krews", "krews", "0.10.9")
    testImplementation("org.junit.jupiter", "junit-jupiter", "5.4.0")
    testCompile("org.assertj", "assertj-core", "3.11.1")
    testImplementation("com.beust", "klaxon", "5.0.1")
}

application {
    mainClassName = "RnaSeqWorkflowKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    archiveBaseName.set(artifactID)
    archiveClassifier.set("exec")
    destinationDirectory.set(file("build"))
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
