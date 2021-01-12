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
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(14)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "0.0.1"
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/kotlin")
        }
        getByName("test") {
            java.srcDir("src/test/kotlin")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", version = Versions.kotlin))
    implementation(project(":adam"))
    implementation(TestLibraries.junit4)
    implementation(Libraries.coroutines)
    implementation("androidx.test:monitor:1.3.0@aar")
}

publishing {
    publications {
        create("aar", MavenPublication::class.java) {
            Deployment.customizePom(project, pom)
            groupId = "com.malinskiy"
            artifactId = "adam-junit4-android"
            version = "0.0.1-SNAPSHOT"
            artifact(buildDir.resolve("outputs/aar/adam-junit4-android-debug.aar"))
        }
    }
}