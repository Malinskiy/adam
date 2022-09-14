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
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

android {
    compileSdk = 30

    defaultConfig {
        minSdk = 14
        targetSdk = 30
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/kotlin")
        }
        getByName("test") {
            java.srcDir("src/test/kotlin")
        }
        getByName("androidTest") {
            java.srcDir("src/androidTest/kotlin")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = "1.5"
    kotlinOptions.languageVersion = "1.5"
}

dependencies {
    implementation(kotlin("stdlib-jdk8", version = Versions.kotlin))
    api(AndroidX.androidXScreenshot)
    api(AndroidX.testMonitor)
    api(TestLibraries.junit4)
}

afterEvaluate {
    Deployment.initialize(project)
}
