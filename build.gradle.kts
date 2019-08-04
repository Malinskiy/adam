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
    compile("net.sf.kxml:kxml2:2.3.0")
    compile("org.jetbrains:annotations:16.0.2")
    implementation(kotlin("stdlib-jdk8"))
    compile("com.google.guava:guava:28.0-jre")

    testCompile("org.easymock:easymock:3.1")
    testCompile("junit:junit:4.12")
}