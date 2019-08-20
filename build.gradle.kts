buildscript {
    repositories {
        jcenter()
        mavenCentral()
        google()
    }
    dependencies {
        classpath(BuildPlugins.kotlinPlugin)
        classpath(BuildPlugins.junitGradle)
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
    implementation(Libraries.guava)
    implementation(Libraries.coroutines)
    implementation(Libraries.ktorNetwork)


    testImplementation(TestLibraries.assertk)
    testImplementation(TestLibraries.easymock)
    testImplementation(TestLibraries.junit)
    testImplementation(kotlin("reflect"))
}