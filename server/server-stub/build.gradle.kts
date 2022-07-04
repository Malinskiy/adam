/*
 * Copyright (C) 2021 Anton Malinskiy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    kotlin("jvm")
    id("jacoco")
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

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = "1.6"
    kotlinOptions.languageVersion = "1.6"
}

dependencies {
    implementation(kotlin("stdlib-jdk8", version = Versions.kotlin))
    implementation(Libraries.coroutines)
    api(project(":adam"))
    api(Libraries.ktorNetwork)
    api(TestLibraries.assertk)

    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.coroutinesDebug)

    integrationTestImplementation(TestLibraries.junit4)
    integrationTestImplementation(TestLibraries.coroutinesDebug)
}
