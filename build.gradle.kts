

val kotestVersion = "5.9.1"
val jacksonVersion = "2.17.2"
val mockkVersion = "1.13.12"
val felleslibVersion = "0.0.140"

plugins {
    id("common")
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    val ktorVersion = libs.versions.ktor.get()
    implementation(libs.rapids.and.rivers)

    implementation(libs.konfig)
    implementation(libs.kotlin.logging)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.10.1")

    implementation(libs.bundles.ktor.client)
    implementation("no.nav.dagpenger:oauth2-klient:2025.02.13-18.02.052b7c34baab")
    implementation(libs.bundles.jackson)
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    // mdc coroutine plugin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.10.1")
    // test
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}

configurations.all {
    // exclude JUnit 4
    exclude(group = "junit", module = "junit")
}

application {
    mainClass.set(" no.nav.dagpenger.andre.ytelser.AppKt")
}
