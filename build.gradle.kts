import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        google()
    }
    dependencies {
        classpath(BuildPlugins.kotlinPlugin)
    }
}

repositories {
    jcenter()
    mavenCentral()
    google()
}

plugins {
    kotlin("jvm")
    id("jacoco")
    id("org.jetbrains.dokka") version Versions.dokka
}

allprojects {
    group = "com.malinskiy"
}

Deployment.initialize(project)

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

fun DependencyHandler.`integrationTestImplementation`(dependencyNotation: Any): Dependency? =
    add("integrationTestImplementation", dependencyNotation)


val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests"
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter("test")

    jacoco {
        include("**")
    }
}
integrationTest.outputs.upToDateWhen { false }

val connectedAndroidTest = task<Test>("connectedAndroidTest") {
    description = "Runs integration tests"
    group = "verification"

    dependsOn(integrationTest)
}

val jacocoIntegrationTestReport = task<JacocoReport>("jacocoIntegrationTestReport") {
    description = "Generates code coverage report for integrationTest task"
    group = "verification"
    reports {
        xml.isEnabled = true
    }

    executionData(integrationTest)
    sourceSets(sourceSets.getByName("integrationTest"))
    classDirectories.setFrom(sourceSets.getByName("main").output.classesDirs)
}
tasks.check { dependsOn(integrationTest, jacocoIntegrationTestReport) }

val jacocoCombinedTestReport = task<JacocoReport>("jacocoCombinedTestReport") {
    description = "Generates code coverage report for all test tasks"
    group = "verification"

    executionData(integrationTest, tasks["test"])
    sourceSets(sourceSets.getByName("integrationTest"), sourceSets.getByName("test"))
    classDirectories.setFrom(sourceSets.getByName("main").output.classesDirs)
    dependsOn(tasks["test"], integrationTest)
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(projectDir.resolve("docs/api"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = "1.4"
}

dependencies {
    implementation(Libraries.kxml)
    implementation(Libraries.annotations)
    implementation(kotlin("stdlib-jdk8", version = Versions.kotlin))
    implementation(Libraries.coroutines)
    implementation(Libraries.ktorNetwork)
    implementation(Libraries.logging)
    implementation(Libraries.pdbank)

    testImplementation(TestLibraries.assertk)
    testImplementation(TestLibraries.junit)
    testImplementation(TestLibraries.imageComparison)
    testImplementation(TestLibraries.coroutinesDebug)
    testImplementation(kotlin("reflect", version = Versions.kotlin))

    integrationTestImplementation(TestLibraries.assertk)
    integrationTestImplementation(TestLibraries.junit)
    integrationTestImplementation(TestLibraries.coroutinesDebug)
    integrationTestImplementation(kotlin("reflect", version = Versions.kotlin))
}
