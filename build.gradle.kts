import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath(BuildPlugins.kotlinPlugin)
        classpath(BuildPlugins.androidGradle)
    }
}

plugins {
    id("com.github.ben-manes.versions") version Versions.gradleVersionsPlugin
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

subprojects {
    repositories {
        mavenCentral()
        google()
    }

    group = "com.malinskiy.adam"
}
