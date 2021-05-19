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

import com.google.protobuf.gradle.builtins
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import com.google.protobuf.gradle.remove

plugins {
    kotlin("jvm")
    id("jacoco")
    id("org.jetbrains.dokka") version Versions.dokka
    id("com.google.protobuf") version Versions.protobufGradle
    id("idea")
}


Deployment.initialize(project)

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.protobuf}"
    }
    plugins {
        id("java") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.grpc}"
        }
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.grpc}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.grpcKotlin}:jdk7@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                remove("java")
            }
            it.plugins {
                id("java") {
                    option("lite")
                }
                id("grpc") {
                    option("lite")
                }
                id("grpckt") {
                    option("lite")
                }
            }
        }
    }
}

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
    outputDirectory.set(rootProject.rootDir.resolve("docs/api"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
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
    api(Libraries.protobufLite)
    api(Libraries.grpcKotlinStubLite)
    api(Libraries.grpcOkhttp)
    implementation(Libraries.javaxAnnotations)
    implementation(Libraries.vertxCore)
    implementation(Libraries.vertxKotlin)
    implementation(Libraries.vertxCoroutines)

    testImplementation(TestLibraries.assertk)
    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.imageComparison)
    testImplementation(kotlin("reflect", version = Versions.kotlin))
    testImplementation(TestLibraries.coroutinesDebug)

    integrationTestImplementation(TestLibraries.coroutinesDebug)
    integrationTestImplementation(TestLibraries.assertk)
    integrationTestImplementation(TestLibraries.junit4)
    integrationTestImplementation(kotlin("reflect", version = Versions.kotlin))
}
