

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

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.9.0")

    implementation(libs.bundles.ktor.client)
    implementation("no.nav.dagpenger:oauth2-klient:2024.10.31-15.02.1d4f08a38d24")
    implementation(libs.bundles.jackson)
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    // mdc coroutine plugin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.9.0")
    // test
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.4.0")
    testImplementation("org.skyscreamer:jsonassert:1.5.3")
}

configurations.all {
    // exclude JUnit 4
    exclude(group = "junit", module = "junit")
}

application {
    mainClass.set(" no.nav.dagpenger.andre.ytelser.AppKt")
}
