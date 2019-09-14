buildscript {
    repositories {
        jcenter()
        mavenCentral()
        google()
    }
    dependencies {
        classpath(BuildPlugins.kotlinPlugin)
        classpath(BuildPlugins.dokka)
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
}

dependencies {
    implementation(Libraries.kxml)
    implementation(Libraries.annotations)
    implementation(kotlin("stdlib-jdk8"))
    implementation(Libraries.coroutines)
    implementation(Libraries.ktorNetwork)
    implementation(Libraries.logging)


    testImplementation(TestLibraries.assertk)
    testImplementation(TestLibraries.junit)
    testImplementation(kotlin("reflect"))
}